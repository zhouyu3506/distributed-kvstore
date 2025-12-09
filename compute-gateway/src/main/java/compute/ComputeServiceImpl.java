package compute;

import compute.proto.ClientDeleteRequest;
import compute.proto.ClientDeleteResponse;
import compute.proto.ClientGetRequest;
import compute.proto.ClientGetResponse;
import compute.proto.ClientPutRequest;
import compute.proto.ClientPutResponse;
import compute.proto.ComputeGrpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import metadata.MetadataGrpc;
import metadata.MetadataOuterClass;
import storage.StorageGrpc;
import storage.StorageOuterClass;

import common.Location;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Compute gateway with metadata cache.
 *
 * Flow:
 *   client -> Compute (this) -> Storage node
 *   Compute uses MetadataCache to map key -> shardId -> Location(host,port)
 */
public class ComputeServiceImpl extends ComputeGrpc.ComputeImplBase {

    private final MetadataGrpc.MetadataBlockingStub metadataStub;
    private final MetadataCache metadataCache = new MetadataCache();

    // cache of storage node id -> stub (reuses channels)
    private final ConcurrentHashMap<String, StorageGrpc.StorageBlockingStub> storageStubCache =
            new ConcurrentHashMap<>();

    // number of shards / storage nodes (used for hash(key) % numShards)
    private volatile int numShards = 0;

    // After CACHE_TTL_MS, we force a refresh; if metadata is down, ops will fail.
    private static final long CACHE_TTL_MS = 20_000; // 20 seconds
    private volatile long lastShardMapRefreshMs = 0L;

    public ComputeServiceImpl(String metadataHost, int metadataPort,
                              String storageHost, int storagePort) {

        // "default" storage stub (optional fallback when metadata is empty)
        ManagedChannel defaultStorageChannel = ManagedChannelBuilder
                .forAddress(storageHost, storagePort)
                .usePlaintext()
                .build();
        StorageGrpc.StorageBlockingStub defaultStub =
                StorageGrpc.newBlockingStub(defaultStorageChannel);
        storageStubCache.put("default", defaultStub);

        // Metadata channel
        ManagedChannel metaChannel = ManagedChannelBuilder
                .forAddress(metadataHost, metadataPort)
                .usePlaintext()
                .build();
        this.metadataStub = MetadataGrpc.newBlockingStub(metaChannel);

        // Best-effort: warm up cache once at startup
        try {
            refreshShardMap();
        } catch (Exception e) {
            System.err.println("[Compute] WARN: failed to pre-load shard map: " + e.getMessage());
        }
    }

    /**
     * Load the full shard map from metadata and update MetadataCache.
     * Uses index in the returned list as shardId: 0..N-1
     */
    private synchronized void refreshShardMap() {
        MetadataOuterClass.ShardMapRequest req =
                MetadataOuterClass.ShardMapRequest.newBuilder().build();

        MetadataOuterClass.ShardMapResponse resp = metadataStub.getShardMap(req);

        Map<Integer, Location> newMap = new HashMap<>();
        int idx = 0;
        for (MetadataOuterClass.NodeInfo n : resp.getNodesList()) {
            Location loc = new Location()
                    .setStorageNodeId(n.getNodeId())
                    .setHost(n.getHost())
                    .setPort(n.getPort())
                    .setShardId(idx);
            newMap.put(idx, loc);

            // lazily ensure a stub exists for this node
            storageStubCache.computeIfAbsent(n.getNodeId(), id -> {
                ManagedChannel ch = ManagedChannelBuilder
                        .forAddress(n.getHost(), n.getPort())
                        .usePlaintext()
                        .build();
                return StorageGrpc.newBlockingStub(ch);
            });

            idx++;
        }

        metadataCache.updateShardMap(metadataCache.getVersion() + 1, newMap);
        numShards = newMap.size();

        // NEW: remember when this refresh happened
        lastShardMapRefreshMs = System.currentTimeMillis();

        // System.out.println("[Compute] shard map updated from metadata, shards=" + numShards);
    }




    /**
     * Resolve which Location (storage node) should serve this key,
     * using the cached shard map. If cache is empty or stale, refresh once.
     */
    private Location resolveLocationForKey(String key) {
        long now = System.currentTimeMillis();

        boolean needRefresh = (numShards <= 0 || metadataCache.getVersion() == 0L);
        // uncomment to disable metadata cache
        // needRefresh = true;


        // TTL: force a refresh after CACHE_TTL_MS even if cache looks fine
        if (!needRefresh && lastShardMapRefreshMs > 0 &&
                (now - lastShardMapRefreshMs) > CACHE_TTL_MS) {
            System.out.println("[Compute] cache TTL expired; refreshing shard map");
            needRefresh = true;
        }

        if (needRefresh) {
            try {
                refreshShardMap();
            } catch (Exception e) {
                System.err.println("[Compute] FATAL: failed to refresh shard map from metadata: "
                        + e.getMessage());
                System.err.println("[Compute] Shutting down compute gateway because metadata is unavailable.");
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                System.exit(1);
            }
        }

        if (numShards <= 0) {
            // still nothing; metadata likely empty
            return null;
        }

        int shardId = Math.abs(key.hashCode()) % numShards;
        Location loc = metadataCache.getLocationForShard(shardId);

        if (loc == null) {
            // possible stale cache; refresh once and retry
            try {
                refreshShardMap();
                loc = metadataCache.getLocationForShard(shardId);
            } catch (Exception e) {
                System.err.println("[Compute] FATAL: failed to refresh shard map on second attempt: "
                        + e.getMessage());
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                System.exit(1);
            }
        }
        return loc;
    }


    /**
     * Get or create a StorageBlockingStub for a given Location.
     */
    private StorageGrpc.StorageBlockingStub resolveStorageStub(Location loc) {
        if (loc == null) {
            // fall back to "default" single-node stub if you want
            return storageStubCache.get("default");
        }

        String nodeId = loc.getStorageNodeId();
        return storageStubCache.computeIfAbsent(nodeId, id -> {
            ManagedChannel ch = ManagedChannelBuilder
                    .forAddress(loc.getHost(), loc.getPort())
                    .usePlaintext()
                    .build();
            return StorageGrpc.newBlockingStub(ch);
        });
    }

    @Override
    public void put(ClientPutRequest request,
                    StreamObserver<ClientPutResponse> responseObserver) {

        String key = request.getKey();
        Location loc = resolveLocationForKey(key);

        ClientPutResponse.Builder out = ClientPutResponse.newBuilder();

        if (loc == null) {
            out.setStatus("NO_NODE");
            responseObserver.onNext(out.build());
            responseObserver.onCompleted();
            return;
        }

        StorageGrpc.StorageBlockingStub stub = resolveStorageStub(loc);
        try {
            StorageOuterClass.PutRequest sreq = StorageOuterClass.PutRequest.newBuilder()
                    .setObjId(key)
                    .setValue(request.getValue())
                    .build();
            StorageOuterClass.PutResponse sresp = stub.put(sreq);
            out.setStatus(sresp.getStatus());
        } catch (Exception e) {
            out.setStatus("ERROR:" + e.getMessage());
        }

        responseObserver.onNext(out.build());
        responseObserver.onCompleted();
    }

    @Override
    public void get(ClientGetRequest request,
                    StreamObserver<ClientGetResponse> responseObserver) {

        String key = request.getKey();
        Location loc = resolveLocationForKey(key);

        ClientGetResponse.Builder out = ClientGetResponse.newBuilder();

        if (loc == null) {
            out.setStatus("NO_NODE");
            responseObserver.onNext(out.build());
            responseObserver.onCompleted();
            return;
        }

        StorageGrpc.StorageBlockingStub stub = resolveStorageStub(loc);
        try {
            StorageOuterClass.GetRequest sreq = StorageOuterClass.GetRequest.newBuilder()
                    .setObjId(key)
                    .build();
            StorageOuterClass.GetResponse sresp = stub.get(sreq);
            out.setStatus(sresp.getStatus());
            if ("OK".equals(sresp.getStatus())) {
                out.setValue(sresp.getValue());
            }
        } catch (Exception e) {
            out.setStatus("ERROR:" + e.getMessage());
        }

        responseObserver.onNext(out.build());
        responseObserver.onCompleted();
    }

    @Override
    public void delete(ClientDeleteRequest request,
                       StreamObserver<ClientDeleteResponse> responseObserver) {

        String key = request.getKey();
        Location loc = resolveLocationForKey(key);

        ClientDeleteResponse.Builder out = ClientDeleteResponse.newBuilder();

        if (loc == null) {
            out.setStatus("NO_NODE");
            responseObserver.onNext(out.build());
            responseObserver.onCompleted();
            return;
        }

        StorageGrpc.StorageBlockingStub stub = resolveStorageStub(loc);
        try {
            StorageOuterClass.DeleteRequest sreq = StorageOuterClass.DeleteRequest.newBuilder()
                    .setObjId(key)
                    .build();
            StorageOuterClass.DeleteResponse sresp = stub.delete(sreq);
            out.setStatus(sresp.getStatus());
        } catch (Exception e) {
            out.setStatus("ERROR:" + e.getMessage());
        }

        responseObserver.onNext(out.build());
        responseObserver.onCompleted();
    }
}
