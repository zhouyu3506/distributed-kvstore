package bench;

import compute.ComputeGrpc;
import compute.ComputeOuterClass;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;


public class ClientBench {

    public static void main(String[] args)  {
        
        if (args.length < 2) {
            System.out.println("Usage: ClientBench <threads> <host1:port1> [mode]");
            System.out.println("  mode: simple or zipf or test");
            return;
        }
        int threads = Integer.parseInt(args[0]);
        String endpoint = args[1];
        String[] parts = endpoint.split(":");
        if (parts.length != 2) {
            System.out.println("Bad endpoint: " + endpoint + " (expected host:port)");
            return;
        }

        String host = parts[0];
        int port = Integer.parseInt(parts[1]);
        String mode = (args.length >= 3) ? args[2].toLowerCase() : "simple";

        ManagedChannel ch = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();

        System.out.println("Added compute target: " + host + ":" + port);

        Stats stats = new Stats();

        // reporter
        Thread reporter = new Thread(new StatsReporter(stats));
        reporter.start();

        // workers
        if ("zipf".equals(mode)) {
            for (int i = 0; i < threads; i++) {
                new Thread(new ZipfWorker(ch, stats, i)).start();
            } 
        } else if ("test".equals(mode)) {
            for (int i = 0; i < threads; i++) {
                new Thread(new TestWorker(ch, stats, i)).start();
            }
        } else {
            for (int i = 0; i < threads; i++) {
                new Thread(new Worker(ch, stats, i)).start();
            }
        }

        System.out.println("ClientBench started with " + threads);


    }
}

