package edu.hse.concurrency.metrics;

public interface Timer {

    void record(long nanos);

    TimerSnapshot snapshot();
}
