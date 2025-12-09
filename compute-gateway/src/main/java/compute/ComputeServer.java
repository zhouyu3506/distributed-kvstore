package compute;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class ComputeServer {
    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
           System.out.println(
                "Usage: ComputeServer <computePort> <metadataHost> <metadataPort> <storageHost> <storagePort> \n" +
                "Not enough arguments provided."
            );
            return;
        }
        int computePort      = Integer.parseInt(args[0]);
        String metadataHost  = args[1];
        int metadataPort     = Integer.parseInt(args[2]);
        String storageHost   = args[3];
        int storagePort      = Integer.parseInt(args[4]);

         Server server = ServerBuilder
                .forPort(computePort)
                .addService(new ComputeServiceImpl(
                        metadataHost,
                        metadataPort,
                        storageHost,
                        storagePort
                ))
                .build()
                .start();

        System.out.println("==== Compute Gateway running on port " + computePort + " ====");
        System.out.println("Metadata: " + metadataHost + ":" + metadataPort);
        System.out.println("Storage : " + storageHost + ":" + storagePort);
        server.awaitTermination();
    }
}
