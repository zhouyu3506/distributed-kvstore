package storage;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import metadata.MetadataGrpc;
import metadata.MetadataOuterClass;;
import java.nio.file.Path;
import java.nio.file.Paths;


public class StorageServer {
    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println(
                "Usage: StorageServer <storageHost> <storagePort> <metadataHost> <metadataPort> \n" +
                "Not enough arguments provided."
            );
            return;
        }
        String host        = args[0];
        int port           = Integer.parseInt(args[1]);
        String metadataHost = args[2];
        int metadataPort    = Integer.parseInt(args[3]);
        String nodeId       = "node" + port;

        String dataDir = "./data-" + port;
        Path snapshotPath = Paths.get(dataDir, "kv-snapshot.bin");
        InMemoryKeyValueStore store =
                new InMemoryKeyValueStore(snapshotPath, /*snapshotEveryN=*/10000);

        // register to metadata-service(arg)
        ManagedChannel metaChannel= ManagedChannelBuilder
                .forAddress(metadataHost, metadataPort)
                        .usePlaintext()
                                .build();
        MetadataGrpc.MetadataBlockingStub metaStub =
                MetadataGrpc.newBlockingStub(metaChannel);

        MetadataOuterClass.RegisterNodeRequest req =
                MetadataOuterClass.RegisterNodeRequest.newBuilder()
                                .setNodeId(nodeId)
                                .setHost(host)
                                .setPort(port)
                                .build();
        MetadataOuterClass.RegisterNodeResponse resp =
                metaStub.registerNode(req);
        System.out.println("Register to metadata: " + resp.getStatus());


        Server server = ServerBuilder
                .forPort(port)
                .addService(new StorageServiceImpl(store))
                .build();

        server.start();

        System.out.println("==== StorageNode running on port " + port + " with snapshot at " + snapshotPath + " ====");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                store.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
        server.awaitTermination();
    }

}
