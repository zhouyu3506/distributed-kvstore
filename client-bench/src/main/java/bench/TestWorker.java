package bench;

import com.google.protobuf.ByteString;
import compute.ComputeGrpc;
import compute.ComputeOuterClass;
import io.grpc.Channel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * Extra test workload:
 * - Mixed GET/PUT/DEL
 * - 35% reads, 50% writes, 15% del
 */

public class TestWorker implements Runnable {

    private final ComputeGrpc.ComputeBlockingStub stub;
    private final Stats stats;
    private final int workerId;
    private final Random random = new Random();

    public TestWorker(Channel channel, Stats stats, int workerId) {
        this.stub = ComputeGrpc.newBlockingStub(channel);
        this.stats = stats;
        this.workerId = workerId;
    }

    @Override
    public void run() {
        List<String> myKeys = new ArrayList<>();
        int counter = 0;

        while (true) {
            long start = System.nanoTime();
            boolean success = false;

            try {
                int op = random.nextInt(100);

                // If we don't have any keys tracked yet, we must PUT first.
                if (myKeys.isEmpty() || op < 50) {
                    // 50% PUT
                    String key = "k" + workerId + "-" + counter;
                    String value = "v" + workerId + "-" + counter;
                    counter++;

                    ComputeOuterClass.ClientPutRequest putReq =
                            ComputeOuterClass.ClientPutRequest.newBuilder()
                                    .setKey(key)
                                    .setValue(ByteString.copyFromUtf8(value))
                                    .build();
                    ComputeOuterClass.ClientPutResponse putResp = stub.put(putReq);

                    success = "OK".equals(putResp.getStatus());
                    if (success) {
                        myKeys.add(key);   // we now *know* this key should exist
                    }

                } else if (op < 85) {
                    // 35% GET on a key we believe exists
                    String key = myKeys.get(random.nextInt(myKeys.size()));
                    ComputeOuterClass.ClientGetRequest getReq =
                            ComputeOuterClass.ClientGetRequest.newBuilder()
                                    .setKey(key)
                                    .build();
                    ComputeOuterClass.ClientGetResponse getResp = stub.get(getReq);

                    // If server says NOT_FOUND here, that's a correctness failure
                    success = "OK".equals(getResp.getStatus());

                } else {
                    // 15% DELETE on a key we believe exists
                    int idx = random.nextInt(myKeys.size());
                    String key = myKeys.get(idx);

                    ComputeOuterClass.ClientDeleteRequest delReq =
                            ComputeOuterClass.ClientDeleteRequest.newBuilder()
                                    .setKey(key)
                                    .build();
                    ComputeOuterClass.ClientDeleteResponse delResp = stub.delete(delReq);

                    success = "OK".equals(delResp.getStatus());
                    if (success) {
                        myKeys.remove(idx);   // no longer expect this key to exist
                    }
                }

            } catch (Exception e) {
                success = false;
            } finally {
                long latency = (System.nanoTime() - start) / 1000;
                stats.record(latency, success);
            }
        }
    }
}
