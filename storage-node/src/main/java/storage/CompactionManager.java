package storage;


public class CompactionManager implements Runnable {

    private volatile boolean running = true;

    @Override
    public void run() {

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
