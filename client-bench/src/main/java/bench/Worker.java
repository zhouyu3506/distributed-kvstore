package bench;

import com.google.protobuf.ByteString;
import compute.ComputeGrpc;
import compute.ComputeOuterClass;
import io.grpc.Channel;

public class Worker implements Runnable {

    private final ComputeGrpc.ComputeBlockingStub stub;

    private final Stats stats;

    private final int workerId;

    public Worker(Channel channel, Stats stats, int workerId) {
        this.stub = ComputeGrpc.newBlockingStub(channel);
        this.stats = stats;
        this.workerId = workerId;
    }

    @Override
    public void run() {
        String key = "k" + workerId;
        String value = "v" + workerId;

        while (true) {
            long start = System.nanoTime();

            try {
                // PUT
                ComputeOuterClass.ClientPutRequest putReq =
                        ComputeOuterClass.ClientPutRequest.newBuilder()
                                .setKey(key).setValue(ByteString.copyFromUtf8(value)).build();
                ComputeOuterClass.ClientPutResponse putResp =
                        stub.put(putReq);

                boolean success = "OK".equals(putResp.getStatus());

                long latency = (System.nanoTime() - start) / 1000;
                stats.record(latency, success);

            } catch (Exception e) {
                long latency = (System.nanoTime() - start)/1000;
                stats.record(latency, false);
            }
        }
    }

}
