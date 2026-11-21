package edu.hse.concurrency.metrics;

public interface Counter {

    void increment();

    long count();
}
