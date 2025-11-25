package storage;


public class CompactionManager implements Runnable {

    private volatile boolean running = true;

    @Override
    public void run() {
        // TODO: implement background compaction logic if needed.
        while (running) {
            try {
                Thread.sleep(10_000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void shutdown() {
        running = false;
    }
}
