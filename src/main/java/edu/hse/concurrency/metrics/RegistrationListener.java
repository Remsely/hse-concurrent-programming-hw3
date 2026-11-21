package edu.hse.concurrency.metrics;

@FunctionalInterface
public interface RegistrationListener {

    void onRegistration(MetricId id, MetricType type);
}
