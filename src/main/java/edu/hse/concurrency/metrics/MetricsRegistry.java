package edu.hse.concurrency.metrics;

import java.util.Map;

public interface MetricsRegistry {

    Counter counter(String name, Map<String, String> tags);

    Gauge gauge(String name, Map<String, String> tags);

    Timer timer(String name, Map<String, String> tags, long... upperBoundsNanos);

    String scrape();

    Registration onRegistration(RegistrationListener listener);
}
