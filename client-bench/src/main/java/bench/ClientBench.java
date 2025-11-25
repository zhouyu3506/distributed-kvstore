package bench;

import compute.ComputeGrpc;
import compute.ComputeOuterClass;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import com.google.protobuf.ByteString;


public class ClientBench {

    public static void main(String[] args)  {
        int threads = 8;
        String host = "localhost";
        int port = 8080;

        // 连接 compute-gateway(默认 8080）
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(host,port)
                .usePlaintext()
                .build();

        Stats stats = new Stats();

        //启动 reporter
        Thread reporter = new Thread(new StatsReporter(stats));
        reporter.start();

        //启动 workers
        for (int i=0; i<threads; i++) {
            new Thread(new Worker(channel, stats,i)).start();
        }

        System.out.println("ClientBench started with " + threads + " threads");


    }
}

