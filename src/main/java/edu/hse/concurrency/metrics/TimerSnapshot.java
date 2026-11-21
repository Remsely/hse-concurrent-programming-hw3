package edu.hse.concurrency.metrics;

import java.util.List;

public record TimerSnapshot(long count, long totalNanos, long maxNanos, List<Long> cumulativeBuckets) {

    public TimerSnapshot {
        cumulativeBuckets = List.copyOf(cumulativeBuckets);
    }
}
