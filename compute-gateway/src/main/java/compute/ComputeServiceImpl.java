package compute;
import compute.proto.ClientDeleteRequest;
import compute.proto.ClientDeleteResponse;
import compute.proto.ClientGetRequest;
import compute.proto.ClientGetResponse;
import compute.proto.ClientPutRequest;
import compute.proto.ClientPutResponse;
import compute.proto.ComputeGrpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import metadata.MetadataGrpc;
import storage.StorageGrpc;
import storage.StorageOuterClass;


/**
 * 最简单版本compute gateway
 * 不做分片，不做metadata
 * 所有请求一律转发到localhost:9000 的单个storageNOde
 */
public class ComputeServiceImpl extends ComputeGrpc.ComputeImplBase {

  private final StorageGrpc.StorageBlockingStub storageStub;

  private final MetadataGrpc.MetadataBlockingStub metadataStub;

  public ComputeServiceImpl() {
      // 连接 StorageNode（最简版仍然保留）
      ManagedChannel storageChannel = ManagedChannelBuilder
              .forAddress("localhost", 9000)
              .usePlaintext()
              .build();
      this.storageStub = StorageGrpc.newBlockingStub(storageChannel);

      // 连接 MetadataService（新增）
      ManagedChannel metaChannel = ManagedChannelBuilder
              .forAddress("localhost", 7080)
              .usePlaintext()
              .build();
      this.metadataStub = MetadataGrpc.newBlockingStub(metaChannel);
  }

  @Override
  public void put(ClientPutRequest request,
                    StreamObserver<ClientPutResponse> responseObserver) {
          StorageOuterClass.PutRequest putReq = StorageOuterClass.PutRequest
                  .newBuilder().setObjId(request.getKey())
                  .setValue(request.getValue())
                  .build();
          StorageOuterClass.PutResponse putResp = storageStub.put(putReq);

          ClientPutResponse resp = ClientPutResponse.newBuilder()
                  .setStatus(putResp.getStatus())
                  .build();
          responseObserver.onNext(resp);
          responseObserver.onCompleted();
  }

  @Override
  public void get(ClientGetRequest request,
                  StreamObserver<ClientGetResponse> responseObserver) {

      StorageOuterClass.GetRequest getReq = StorageOuterClass.GetRequest.newBuilder()
              .setObjId(request.getKey())
              .build();

      StorageOuterClass.GetResponse getResp = storageStub.get(getReq);

      ClientGetResponse.Builder builder = ClientGetResponse.newBuilder()
              .setStatus(getResp.getStatus());

      if ("OK".equals(getResp.getStatus())) {
          builder.setValue(getResp.getValue());
      }

      responseObserver.onNext(builder.build());
      responseObserver.onCompleted();
  }

  @Override
    public void delete(ClientDeleteRequest request,
                       StreamObserver<ClientDeleteResponse> responseObserver) {
      StorageOuterClass.DeleteRequest delReq = StorageOuterClass
              .DeleteRequest.newBuilder().setObjId(request.getKey())
              .build();

      StorageOuterClass.DeleteResponse delResp = storageStub.delete(delReq);

      ClientDeleteResponse resp = ClientDeleteResponse.newBuilder()
              .setStatus(delResp.getStatus())
              .build();

      responseObserver.onNext(resp);
      responseObserver.onCompleted();

  }














}
