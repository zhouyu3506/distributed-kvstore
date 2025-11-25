package bench;

import java.util.concurrent.atomic.AtomicLong;

/*
多线程压测场景下
记录吞吐量，延迟，成功数，失败数
 */
public class Stats {
    public AtomicLong totalOps = new AtomicLong();   // the total running times
    public AtomicLong successOps = new AtomicLong();   // the total successful times
    public AtomicLong failedOps = new AtomicLong();    // the total failure times
    public AtomicLong totalLatencyMicros = new AtomicLong();   // the total latency time

    public void record (long latencyMicros, boolean success) {
        totalOps.incrementAndGet();
        totalLatencyMicros.addAndGet(latencyMicros);
        if (success) {
            successOps.incrementAndGet();
        } else {
            failedOps.incrementAndGet();
        }
    }

    public Stats snapshotAndReset() {
        Stats copy = new Stats();
        copy.totalOps.set(totalOps.getAndSet(0));
        copy.successOps.set(successOps.getAndSet(0));
        copy.failedOps.set(failedOps.getAndSet(0));
        copy.totalLatencyMicros.set(totalLatencyMicros.getAndSet(0));
        return copy;
    }

}
