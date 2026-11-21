package edu.hse.concurrency.metrics;

public interface Gauge {

    void set(long value);

    long get();
}
