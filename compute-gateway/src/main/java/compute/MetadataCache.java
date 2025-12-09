package compute;

import common.Location;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class MetadataCache {

    private final ConcurrentHashMap<Integer, Location> shardMap = new ConcurrentHashMap<>();
    private volatile long version = 0L;

    public Location getLocationForShard(int shardId) {
        return shardMap.get(shardId);
    }

    public void updateShardMap(long newVersion, Map<Integer, Location> newMap) {
        this.version = newVersion;
        shardMap.clear();
        shardMap.putAll(newMap);
    }

    public long getVersion() {
        return version;
    }
}
