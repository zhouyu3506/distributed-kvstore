package metadata;

/**
 * Handles taking and loading snapshots of the metadata state.
 */
public class SnapshotManager {

    public void saveSnapshot() {
        // TODO: serialize shard map and membership info to disk
    }

    public void loadSnapshotIfExists() {
        // TODO: read snapshot from disk and restore state
    }
}
