package metadata;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class MetadataServer {
    public static void main(String[] args) throws Exception {
        MetadataState state = new MetadataState();

        Server server = ServerBuilder.forPort(7080)
                .addService(new MetadataServiceImpl(state))
                .build();

        System.out.println("====MetadataService running on port 7080====");
        server.start();
        server.awaitTermination();
    }
}
