package edu.hse.concurrency.metrics;

public interface Registration extends AutoCloseable {

    @Override
    void close();
}
