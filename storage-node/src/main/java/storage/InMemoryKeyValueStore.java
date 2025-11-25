package storage;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.Objects;

public class InMemoryKeyValueStore implements KeyValueStore {

  private final ConcurrentMap<String,byte[]> map = new ConcurrentHashMap<>();

  @Override
    public void put(String objId, byte[] value) {
      //1.check the parameter
      Objects.requireNonNull(objId, "objId must not be null");
      Objects.requireNonNull(value, "value must not be null");

      //2. create or override the value
      map.put(objId, value);

      //3.内存实现不需要commit/wal,持久化实现时可以在这里做commit
  }

  @Override
    public byte[] get(String objId) {
      Objects.requireNonNull(objId, "objId must not be null");
      return map.get(objId);
  }

  @Override
    public boolean delete(String objId) {
      Objects.requireNonNull(objId, "objId must not be null");
      return map.remove(objId) != null;  //if you delete the not existing key, it will be a mistake
  }


}
