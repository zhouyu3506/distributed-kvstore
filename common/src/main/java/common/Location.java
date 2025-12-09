package common;

public class Location {
    private String storageNodeId;

    private String host;

    private int port;

    private int shardId;

    public String getStorageNodeId() {
        return storageNodeId;
    }

    public Location setStorageNodeId(String storageNodeId) {
        this.storageNodeId = storageNodeId;
        return this;
    }

    public String getHost() {
        return host;
    }

    public Location setHost(String host) {
        this.host = host;
        return this;
    }

    public int getPort() {
        return port;
    }

    public Location setPort(int port) {
        this.port = port;
        return this;
    }

    public int getShardId() {
        return shardId;
    }

    public Location setShardId(int shardId) {
        this.shardId = shardId;
        return this;
    }

    @Override
    public String toString() {
        return "Location{" +
                "storageNodeId='" + storageNodeId + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", shardId=" + shardId +
                '}';
    }

}
