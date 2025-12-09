package storage;

public interface KeyValueStore {


    // put
    void put(String objId, byte[] value);

    //get
    byte[] get(String objId);

    //delete
    boolean delete(String objId);

    void close();
}
