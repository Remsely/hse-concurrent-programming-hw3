package edu.hse.concurrency.metrics;

import java.util.Map;

public class MetricsRegistryImpl implements MetricsRegistry {

    @Override
    public Counter counter(String name, Map<String, String> tags) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public Gauge gauge(String name, Map<String, String> tags) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public Timer timer(String name, Map<String, String> tags, long... upperBoundsNanos) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public String scrape() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public Registration onRegistration(RegistrationListener listener) {
        throw new UnsupportedOperationException("TODO");
    }
}
