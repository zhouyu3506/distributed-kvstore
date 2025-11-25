package bench;


/**
 * Collects latency samples and prints basic statistics.
 */
public class StatsReporter implements Runnable{
    private final Stats stats;

    public StatsReporter(Stats stats) {
        this.stats = stats;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(1000);
                Stats snap = stats.snapshotAndReset();
                long ops = snap.totalOps.get();
                long succ = snap.successOps.get();
                long fail = snap.failedOps.get();

                long avgLatency = succ >0 ? snap.totalLatencyMicros.get() / succ:0;

                System.out.printf("[REPORT] ops=%d, success=%d, fail=%d, avgLatency=%dus\n",
                        ops, succ, fail, avgLatency);
            } catch (InterruptedException e) {
                return;
            }
        }
    }
}
