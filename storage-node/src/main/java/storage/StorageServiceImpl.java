package storage;

import io.grpc.stub.StreamObserver;

public class StorageServiceImpl extends StorageGrpc.StorageImplBase {
  private final KeyValueStore store;

  public StorageServiceImpl (KeyValueStore store) {
      this.store = store;
  }

  @Override
    public void put(StorageOuterClass.PutRequest request,
                    StreamObserver<StorageOuterClass.PutResponse> responseObserver) {
      try {
          String objId = request.getObjId();
          byte[] value = request.getValue().toByteArray();

          store.put(objId, value);
          StorageOuterClass.PutResponse resp =
                  StorageOuterClass.PutResponse.newBuilder()
                          .setStatus("OK").build();
          responseObserver.onNext(resp);
          responseObserver.onCompleted();
      } catch (Exception e) {
          responseObserver.onError(e);
      }
  }

  @Override
    public void get(StorageOuterClass.GetRequest request,
                    StreamObserver<StorageOuterClass.GetResponse> responseObserver) {
      String objId = request.getObjId();
      byte[] value = store.get(objId);

      StorageOuterClass.GetResponse.Builder builder =
              StorageOuterClass.GetResponse.newBuilder();

      if (value != null) {
          builder.setStatus("OK")
                  .setValue(com.google.protobuf.ByteString.copyFrom(value));
      } else {
          builder.setStatus("NOT_FOUND");
      }

      responseObserver.onNext(builder.build());
      responseObserver.onCompleted();
  }

  @Override
    public void delete(StorageOuterClass.DeleteRequest request,
                       StreamObserver<StorageOuterClass.DeleteResponse> responseObserver) {
      boolean deleted = store.delete(request.getObjId());

      StorageOuterClass.DeleteResponse resp =
              StorageOuterClass.DeleteResponse.newBuilder()
                      .setStatus(deleted ? "OK" : "NOT_FOUND").build();
      responseObserver.onNext(resp);
      responseObserver.onCompleted();

  }






}
