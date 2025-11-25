

# 🔷 1. Overall Architecture

```text
                          (Load Testing / Client)
+-----------------+        gRPC: compute.proto        +--------------------+
|  client-bench   |  <----------------------------->  |   compute-gateway  |
| (ClientBench)   |           port 8080              |  (ComputeServer)   |
+-----------------+                                   +---------+----------+
                                                                |
                                                                | gRPC: metadata.proto
                                                                | (fetch shard map / nodes)
                                                                v
                                                       +--------+---------+
                                                       |   metadata-      |
                                                       |    service       |
                                                       | (MetadataServer) |
                                                       +--------+---------+
                                                                ^
                                                                | registerNode()
                                                                | port 7000/7080
                  gRPC: storage.proto                           |
+-------------------+   (Put/Get/Delete)               +--------+----------+
|  storage-node #1  | <------------------------------> |   storage-node #N |
| (StorageServer)   |                                    | (multiple nodes) |
+-------------------+                                    +------------------+
```

**Request flow:**

1. `client-bench` sends PUT/GET/DELETE → `compute-gateway`
2. `compute-gateway`

   * fetches shard/node info from metadata-service (cached locally)
   * forwards the request to the correct storage-node
3. `storage-node` registers itself to metadata-service at startup

---

# 🔷 2. Metadata-Service Architecture

```text
+-----------------------------+
|        MetadataServer       |
|  - main()                   |
|  - new MetadataState()      |
|  - starts gRPC server 7080  |
|    .addService(             |
|        new MetadataServiceImpl(state)
|      )                      |
+-----------------------------+
              |
              v
+-----------------------------+
|     MetadataServiceImpl     |
|  gRPC implementation:       |
|  - registerNode()           |
|  - getShardMap()            |
|  (extendable: heartbeat…)   |
|  Uses MetadataState         |
+-----------------------------+
              |
              v
+-----------------------------+
|        MetadataState        |
|  - Map<shardId, Location>   |
|  - version                  |
|  - registerNode()           |
|  - updateShardMap()         |
|  (extend: persist to JSON)  |
+-----------------------------+

(Optional)
+-----------------------------+
|      SnapshotManager        |
|  - Persist MetadataState →  |
|       metadata.json         |
|  - Load on startup          |
+-----------------------------+
```

---

# 🔷 3. Storage-Node Architecture

```text
+-----------------------------+
|         StorageServer       |
|  - main()                   |
|  - new InMemoryKeyValueStore|
|  - before start:            |
|       connect to metadata   |
|       registerNode(...)     |
|  - start gRPC server 9000   |
|    .addService(             |
|        new StorageServiceImpl(store)
|      )                      |
+-----------------------------+
              |
              v
+-----------------------------+
|     StorageServiceImpl      |
|  gRPC implementation:       |
|  - put()                    |
|  - get()                    |
|  - delete()                 |
|  Uses KeyValueStore         |
+-----------------------------+
              |
              v
+-----------------------------+
|       KeyValueStore         |
|  - put(key, value)          |
|  - get(key)                 |
|  - delete(key)              |
+-----------------------------+
              ^
              |
+-----------------------------+
|   InMemoryKeyValueStore     |
|  - HashMap<String, byte[]>  |
|  (extend: RocksDB/file)     |
+-----------------------------+

(Optional)
+-----------------------------+
|      CompactionManager      |
|  - Data cleanup/compaction  |
+-----------------------------+
```

---

# 🔷 4. Compute-Gateway Architecture

```text
+-----------------------------+
|        ComputeServer        |
|  - main()                   |
|  - start gRPC server 8080   |
|    .addService(             |
|        new ComputeServiceImpl(...)
|      )                      |
+-----------------------------+
              |
              v
+-----------------------------+
|     ComputeServiceImpl      |
|  - PUT/GET/DELETE handlers  |
|  - Logic flow:              |
|       1) Determine shard    |
|          via MetadataCache  |
|       2) Contact metadata   |
|          service if needed  |
|       3) Forward request to |
|          correct storage    |
|       4) Return response    |
+-----------------------------+
              |
              v
+-----------------------------+
|        MetadataCache        |
|  - Map<shardId, Location>   |
|  - version                  |
|  - updateShardMap()         |
|  - getLocationForKey()      |
|  (extend: TTL, refresh…)    |
+-----------------------------+
```

---

# 🔷 5. Client-Bench Architecture

```text
+-----------------------------+
|         ClientBench         |
|  - main()                   |
|  - create channel →         |
|        compute-gateway:8080 |
|  - create shared Stats      |
|  - start StatsReporter      |
|  - start N Worker threads   |
+-----------------------------+
       |                  |
       | uses             | uses
       v                  v
+----------------+   +----------------------+
|     Stats      |   |    StatsReporter     |
|  - totalOps    |   |  - print QPS         |
|  - successOps  |   |  - print latency     |
|  - failedOps   |   +----------------------+
|  - latencySum  |
+----------------+
       ^
       |
       | updated by
       v
+-----------------------------+
|           Worker            |
|  - loop:                    |
|     * generate key/value    |
|     * PUT or GET request    |
|     * measure latency       |
|     * update Stats          |
+-----------------------------+

I can generate it fully formatted in Markdown for your GitHub repo.
