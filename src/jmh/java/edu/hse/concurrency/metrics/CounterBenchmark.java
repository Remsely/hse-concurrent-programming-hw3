package edu.hse.concurrency.metrics;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 7, time = 2)
public class CounterBenchmark {

    @State(Scope.Benchmark)
    public static class Baselines {
        private long value;
        private final AtomicLong atomic = new AtomicLong();

        synchronized void increment() {
            value++;
        }
    }

    @State(Scope.Benchmark)
    public static class RegistryCounter {
        private final Counter counter = new MetricsRegistryImpl().counter("benchmark_total", Map.of());
    }

    @Benchmark
    public void monitor(Baselines counters) {
        counters.increment();
    }

    @Benchmark
    public void atomicLong(Baselines counters) {
        counters.atomic.incrementAndGet();
    }

    @Benchmark
    public void registryCounter(RegistryCounter counter) {
        counter.counter.increment();
    }
}
