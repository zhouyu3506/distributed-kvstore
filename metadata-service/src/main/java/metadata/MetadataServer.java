package metadata;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class MetadataServer {
    public static void main(String[] args) throws Exception {
        MetadataState state = new MetadataState();
        if (args.length < 1) {
           System.out.println(
                "Usage: MetadataServer <metadataPort> \n" +
                "Not enough arguments provided."
            );
            return;
        }
        int port = Integer.parseInt(args[0]);

        Server server = ServerBuilder.forPort(port)
                .addService(new MetadataServiceImpl(state))
                .build();

        System.out.println("==== MetadataService running on port " + port + " ====");
        server.start();
        server.awaitTermination();
    }
}
