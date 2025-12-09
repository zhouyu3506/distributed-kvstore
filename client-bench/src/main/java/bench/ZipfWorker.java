package bench;

import com.google.protobuf.ByteString;
import compute.ComputeGrpc;
import compute.ComputeOuterClass;
import io.grpc.Channel;

import java.util.Random;

/**
 * Extra test workload:
 * - Mixed GET/PUT
 * - 70% reads, 30% writes
 * - 1-4 KB values
 * - Zipf-like hot-spot on keys
 */
public class ZipfWorker implements Runnable {

    private final ComputeGrpc.ComputeBlockingStub stub;
    private final Stats stats;
    private final int workerId;
    private final Random random;

    // ---- Workload configuration ----

    // 70% reads, 30% writes
    private static final double READ_RATIO = 0.7;

    // Key space and Zipf-like hot-spot
    private static final int KEY_SPACE = 1_000_000; // total distinct keys
    private static final int HOT_KEYS  = 10_000;    // top 1% are "hot"
    private static final double HOT_PROB = 0.8;     // 80% of ops hit hot set

    // Value size (1-4 KB)
    private static final int MIN_VALUE_SIZE = 1024; // 1 KB
    private static final int MAX_VALUE_SIZE = 4096; // 4 KB

    public ZipfWorker(Channel channel, Stats stats, int workerId) {
        this.stub = ComputeGrpc.newBlockingStub(channel);
        this.stats = stats;
        this.workerId = workerId;
        this.random = new Random(workerId * 2025L);
    }

    @Override
    public void run() {
        while (true) {
            long start = System.nanoTime();
            try {
                boolean isRead = random.nextDouble() < READ_RATIO;
                String key = pickZipfLikeKey();

                if (isRead) {
                    // -------- GET --------
                    ComputeOuterClass.ClientGetRequest getReq =
                            ComputeOuterClass.ClientGetRequest.newBuilder()
                                    .setKey(key)
                                    .build();

                    ComputeOuterClass.ClientGetResponse getResp = stub.get(getReq);
                    String status = getResp.getStatus();
                    boolean success = "OK".equals(status) || "NOT_FOUND".equals(status);

                    long latencyMicros = (System.nanoTime() - start) / 1000;
                    stats.record(latencyMicros, success);

                } else {
                    // -------- PUT --------
                    byte[] valueBytes = randomValue();
                    ByteString value = ByteString.copyFrom(valueBytes);

                    ComputeOuterClass.ClientPutRequest putReq =
                            ComputeOuterClass.ClientPutRequest.newBuilder()
                                    .setKey(key)
                                    .setValue(value)
                                    .build();

                    ComputeOuterClass.ClientPutResponse putResp = stub.put(putReq);
                    boolean success = "OK".equals(putResp.getStatus());

                    long latencyMicros = (System.nanoTime() - start) / 1000;
                    stats.record(latencyMicros, success);
                }

            } catch (Exception e) {
                long latencyMicros = (System.nanoTime() - start) / 1000;
                stats.record(latencyMicros, false);
            }
        }
    }

    // -------- Helpers --------

    // "Zipf-like": 80% of accesses go to first HOT_KEYS, rest spread over others.
    private String pickZipfLikeKey() {
        int id;
        if (random.nextDouble() < HOT_PROB) {
            // hot set [0, HOT_KEYS)
            id = random.nextInt(HOT_KEYS);
        } else {
            // cold set [HOT_KEYS, KEY_SPACE)
            id = HOT_KEYS + random.nextInt(KEY_SPACE - HOT_KEYS);
        }
        return "key-" + id;
    }

    // Generate a random value with size uniformly in [1KB, 4KB].
    private byte[] randomValue() {
        int sizeRange = MAX_VALUE_SIZE - MIN_VALUE_SIZE + 1;
        int size = MIN_VALUE_SIZE + random.nextInt(sizeRange);
        byte[] buf = new byte[size];
        random.nextBytes(buf);
        return buf;
    }
}
