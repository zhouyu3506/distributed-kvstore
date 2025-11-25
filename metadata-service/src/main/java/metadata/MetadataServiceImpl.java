package metadata;

import io.grpc.stub.StreamObserver;
import metadata.MetadataOuterClass.*;

public class MetadataServiceImpl extends MetadataGrpc.MetadataImplBase {

  private final MetadataState state;

  public MetadataServiceImpl(MetadataState state) {
      this.state = state;
  }

  @Override
    public void registerNode(RegisterNodeRequest request,
                             StreamObserver<RegisterNodeResponse> responseObserver) {
      state.register(request.getNodeId(), request.getHost(), request.getPort());

      RegisterNodeResponse resp = RegisterNodeResponse.newBuilder()
              .setStatus("OK")
              .build();
      responseObserver.onNext(resp);
      responseObserver.onCompleted();
  }

  @Override
    public void getShardMap(ShardMapRequest request,
                            StreamObserver<ShardMapResponse> responseObserver
                            ) {
      ShardMapResponse.Builder builder = ShardMapResponse.newBuilder();

      for (MetadataState.NodeInfo n : state.listNodes()) {
          builder.addNodes(NodeInfo.newBuilder()
                  .setNodeId(n.nodeId)
                  .setHost(n.host)
                  .setPort(n.port)
                  .build());
      }

      responseObserver.onNext(builder.build());
      responseObserver.onCompleted();
  }

  @Override
    public void lookupKey(LookupRequest request,
                          StreamObserver<LookupResponse> responseObserver) {
      MetadataState.NodeInfo n = state.lookup(request.getKey());
      LookupResponse.Builder builder = LookupResponse.newBuilder();

      if (n != null) {
          builder.setStatus("OK")
                  .setNodeId(n.nodeId)
                  .setHost(n.host)
                  .setPort(n.port);
      } else {
          builder.setStatus("NOT_FOUND");
      }

      responseObserver.onNext(builder.build());
      responseObserver.onCompleted();
  }



}
