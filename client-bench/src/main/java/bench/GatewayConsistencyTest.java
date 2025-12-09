package bench;

import com.google.protobuf.ByteString;
import compute.ComputeGrpc;
import compute.ComputeOuterClass;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class GatewayConsistencyTest {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: GatewayConsistencyTest <gw1Host:gw1Port> <gw2Host:gw2Port>");
            return;
        }

        String[] p1 = args[0].split(":");
        String[] p2 = args[1].split(":");
        String gw1Host = p1[0];
        int gw1Port = Integer.parseInt(p1[1]);
        String gw2Host = p2[0];
        int gw2Port = Integer.parseInt(p2[1]);

        ManagedChannel ch1 = ManagedChannelBuilder
                .forAddress(gw1Host, gw1Port)
                .usePlaintext()
                .build();
        ManagedChannel ch2 = ManagedChannelBuilder
                .forAddress(gw2Host, gw2Port)
                .usePlaintext()
                .build();

        ComputeGrpc.ComputeBlockingStub gw1 = ComputeGrpc.newBlockingStub(ch1);
        ComputeGrpc.ComputeBlockingStub gw2 = ComputeGrpc.newBlockingStub(ch2);

        String key = "consistency-test-key";
        String v1  = "value-from-gateway-1";
        String v2  = "value-from-gateway-2";

        try {
            // Clean up any old value (ignore errors)
            try {
                ComputeOuterClass.ClientDeleteRequest delReq =
                        ComputeOuterClass.ClientDeleteRequest.newBuilder()
                                .setKey(key)
                                .build();
                gw1.delete(delReq);
            } catch (Exception ignored) {}

            // 1) PUT through GW1
            System.out.println("PUT via GW1: key=" + key + ", value=" + v1);
            ComputeOuterClass.ClientPutRequest putReq1 =
                    ComputeOuterClass.ClientPutRequest.newBuilder()
                            .setKey(key)
                            .setValue(ByteString.copyFromUtf8(v1))
                            .build();
            ComputeOuterClass.ClientPutResponse putResp1 = gw1.put(putReq1);
            System.out.println("  status=" + putResp1.getStatus());

            // 2) GET through GW2
            System.out.println("GET via GW2:");
            ComputeOuterClass.ClientGetRequest getReq2 =
                    ComputeOuterClass.ClientGetRequest.newBuilder()
                            .setKey(key)
                            .build();
            ComputeOuterClass.ClientGetResponse getResp2 = gw2.get(getReq2);
            System.out.println("  status=" + getResp2.getStatus());
            if ("OK".equals(getResp2.getStatus())) {
                System.out.println("  value=" + getResp2.getValue().toStringUtf8());
            }

            // 3) Overwrite via GW2
            System.out.println("PUT via GW2: key=" + key + ", value=" + v2);
            ComputeOuterClass.ClientPutRequest putReq2 =
                    ComputeOuterClass.ClientPutRequest.newBuilder()
                            .setKey(key)
                            .setValue(ByteString.copyFromUtf8(v2))
                            .build();
            ComputeOuterClass.ClientPutResponse putResp2 = gw2.put(putReq2);
            System.out.println("  status=" + putResp2.getStatus());

            // 4) GET via GW1 again
            System.out.println("GET via GW1:");
            ComputeOuterClass.ClientGetRequest getReq1b =
                    ComputeOuterClass.ClientGetRequest.newBuilder()
                            .setKey(key)
                            .build();
            ComputeOuterClass.ClientGetResponse getResp1b = gw1.get(getReq1b);
            System.out.println("  status=" + getResp1b.getStatus());
            if ("OK".equals(getResp1b.getStatus())) {
                System.out.println("  value=" + getResp1b.getValue().toStringUtf8());
            }

        } finally {
            ch1.shutdown();
            ch2.shutdown();
        }
    }
}
z