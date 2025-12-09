package storage;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.Objects;

public class InMemoryKeyValueStore implements KeyValueStore {

    private final ConcurrentMap<String, byte[]> map = new ConcurrentHashMap<>();
    private final File snapshotFile;
    private final int snapshotEveryN;
    private int pendingWrites = 0;

    public InMemoryKeyValueStore(Path snapshotPath, int snapshotEveryN) {
        this.snapshotFile = snapshotPath.toFile();
        this.snapshotEveryN = Math.max(1, snapshotEveryN);
        File parent = this.snapshotFile.getParentFile();
        if (parent != null && !parent.exists()) {
            boolean ok = parent.mkdirs();
            if (!ok) {
                System.err.println("[SnapshotKV] WARNING: failed to create directory " + parent.getAbsolutePath());
            } else {
                System.out.println("[SnapshotKV] Created snapshot directory " + parent.getAbsolutePath());
            }
        }
        loadSnapshotIfExists();
    }

    @SuppressWarnings("unchecked")
    private void loadSnapshotIfExists() {
        if (!snapshotFile.exists()) {
            System.out.println("[SnapshotKV] No snapshot file, starting empty");
            return;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(snapshotFile))) {
            Object obj = ois.readObject();
            // old-style instanceof + cast, to avoid newer pattern-matching syntax
            if (obj instanceof Map<?, ?>) {
                Map<?, ?> loaded = (Map<?, ?>) obj;
                map.clear();
                for (Map.Entry<?, ?> e : loaded.entrySet()) {
                    Object k = e.getKey();
                    Object v = e.getValue();
                    if (k instanceof String && v instanceof byte[]) {
                        map.put((String) k, (byte[]) v);
                    }
                }
                System.out.println("[SnapshotKV] Loaded " + map.size() + " entries from snapshot");
            }
        } catch (Exception e) {
            System.err.println("[SnapshotKV] Failed to load snapshot: " + e);
        }
    }

    private synchronized void maybeSaveSnapshot() {
        pendingWrites++;
        if (pendingWrites < snapshotEveryN) {
            return;
        }
        pendingWrites = 0;
        saveSnapshot();
    }

    private void saveSnapshot() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(snapshotFile))) {
            // write a shallow copy of the map to avoid concurrent modification issues
            oos.writeObject(new ConcurrentHashMap<>(map));
            oos.flush();
        } catch (Exception e) {
            System.err.println("[SnapshotKV] Failed to save snapshot: " + e);
        }
    }

    @Override
    public void put(String key, byte[] value) {
        Objects.requireNonNull(key, "objId must not be null");
        Objects.requireNonNull(value, "value must not be null");
        map.put(key, value);
        maybeSaveSnapshot();
    }

    @Override
    public byte[] get(String key) {
        Objects.requireNonNull(key, "objId must not be null");
        return map.get(key);
    }

    @Override
    public boolean delete(String key) {
        byte[] removed = map.remove(key);
        if (removed != null) {
            maybeSaveSnapshot();
            return true;
        }
        return false;
    }

    @Override
    public void close() {
        saveSnapshot();
    }
}
