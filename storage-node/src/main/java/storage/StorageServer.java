package storage;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import metadata.MetadataGrpc;
import metadata.MetadataOuterClass;


public class StorageServer {
    public static void main(String[] args) throws Exception {
      String nodeId = "node-9000";
      String host = "localhost";
      int port = 9000;

      //1.启动前注册到metadata-service(7000)
      ManagedChannel metaChannel= ManagedChannelBuilder
              .forAddress("localhost",7080)
                      .usePlaintext()
                              .build();
      MetadataGrpc.MetadataBlockingStub metaStub =
              MetadataGrpc.newBlockingStub(metaChannel);

      MetadataOuterClass.RegisterNodeRequest req =
              MetadataOuterClass.RegisterNodeRequest.newBuilder()
                              .setNodeId(nodeId)
                              .setHost(host)
                               .setPort(port)
                               .build();
      MetadataOuterClass.RegisterNodeResponse resp =
              metaStub.registerNode(req);
      System.out.println("Register to metadata: " + resp.getStatus());


      Server server = ServerBuilder
              .forPort(port)
              .addService(new StorageServiceImpl(new InMemoryKeyValueStore()))
              .build();

      System.out.println("StorageNode running at port " + port);
      server.start();

      System.out.println("StorageNode is running at port " + port);
      server.awaitTermination();
    }





}
