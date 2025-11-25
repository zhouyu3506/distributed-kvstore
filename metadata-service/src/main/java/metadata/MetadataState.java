package metadata;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MetadataState {
    // 所有 storage nodes;
    private final Map<String,NodeInfo> nodes =
            new ConcurrentHashMap<>();

    public static class NodeInfo {
        public final String nodeId;

        public final String host;

        public final int port;

        public NodeInfo(String nodeId, String host, int port) {
            this.nodeId = nodeId;
            this.host = host;
            this.port = port;
        }
    }

    public void register(String nodeId, String host, int port) {
        nodes.put(nodeId, new NodeInfo(nodeId, host, port));
    }

    public List<NodeInfo> listNodes() {
        return new ArrayList<>(nodes.values());
    }

    //简化：按key.hash % N 选storage node
    public NodeInfo lookup(String key) {
        List<NodeInfo> list = listNodes();
        if (list.isEmpty()) {
            return null;
        }
        int idx = Math.abs(key.hashCode()) % list.size();
        return list.get(idx);
    }


}
