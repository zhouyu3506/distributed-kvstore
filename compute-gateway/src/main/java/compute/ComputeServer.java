package compute;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class ComputeServer {
    public static void main(String[] args) throws Exception {
        Server server = ServerBuilder
                .forPort(8080)
                .addService(new ComputeServiceImpl())
                .build()
                .start();

        System.out.println("Compute Gateway started on port 8080");
        server.awaitTermination();
    }
}
