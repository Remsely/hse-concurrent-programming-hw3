package edu.hse.concurrency.metrics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Timeout(8)
class MetricsSmokeTest {

    private final MetricsRegistry registry = new MetricsRegistryImpl();

    @Test
    void concurrentRegistrationReturnsOneCounterAndKeepsIncrements() throws Exception {
        var start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var tasks = new ArrayList<Future<Counter>>();
            for (int thread = 0; thread < 4; thread++) {
                tasks.add(executor.submit(() -> {
                    start.await();
                    Counter counter = registry.counter("requests_total", Map.of("route", "/orders"));
                    for (int i = 0; i < 1000; i++) {
                        counter.increment();
                    }
                    return counter;
                }));
            }
            start.countDown();
            Counter counter = tasks.getFirst().get();
            for (var task : tasks) {
                assertSame(counter, task.get());
            }
            assertEquals(4000, counter.count());
        }
    }

    @Test
    void registryCopiesTagsAndGaugeAcceptsNegativeValues() {
        var tags = new HashMap<>(Map.of("node", "a"));
        Gauge gauge = registry.gauge("queue_delta", tags);
        tags.put("node", "b");

        assertSame(gauge, registry.gauge("queue_delta", Map.of("node", "a")));
        assertEquals(0, gauge.get());
        gauge.set(-4);
        assertEquals(-4, gauge.get());
    }

    @Test
    void timerIncludesBoundaryAndKeepsOldSnapshots() {
        long[] bounds = {1_000_000, 3_000_000};
        Timer timer = registry.timer("request_seconds", Map.of(), bounds);
        bounds[0] = 500_000;
        assertEquals(new TimerSnapshot(0, 0, 0, List.of(0L, 0L, 0L)), timer.snapshot());

        timer.record(1_000_000);
        timer.record(2_000_000);
        timer.record(4_000_000);
        TimerSnapshot snapshot = timer.snapshot();
        timer.record(0);

        assertEquals(new TimerSnapshot(3, 7_000_000, 4_000_000, List.of(1L, 2L, 3L)), snapshot);
        assertEquals(new TimerSnapshot(4, 7_000_000, 4_000_000, List.of(2L, 3L, 4L)), timer.snapshot());
        assertSame(timer, registry.timer("request_seconds", Map.of(), 1_000_000, 3_000_000));
        assertThrows(IllegalArgumentException.class,
                () -> registry.timer("request_seconds", Map.of("route", "/orders"), 2_000_000));
        assertThrows(IllegalArgumentException.class, () -> timer.record(-1));
    }

    @Test
    void scrapeWritesSecondsAndEscapesTags() {
        registry.counter("requests_total", Map.of("route", "a\\b\"c\nd")).increment();
        registry.timer("request_seconds", Map.of("route", "/orders"), 1_000_000).record(500_000);

        String scrape = registry.scrape();
        var lines = scrape.lines()
                .map(line -> line.strip().replaceAll("[ \t]+", " ")
                        .replaceAll("\\{le=\"([^\"]+)\",route=\"/orders\"}", "{route=\"/orders\",le=\"$1\"}"))
                .toList();
        assertTrue(lines.contains("# TYPE requests_total counter"));
        assertTrue(lines.contains("# TYPE request_seconds histogram"));
        assertSample(lines, "requests_total{route=\"a\\\\b\\\"c\\nd\"}", "1");
        assertSample(lines, "request_seconds_count{route=\"/orders\"}", "1");
        assertSample(lines, "request_seconds_sum{route=\"/orders\"}", "0.0005");

        var buckets = lines.stream().filter(line -> line.startsWith("request_seconds_bucket{")).toList();
        assertEquals(2, buckets.size());
        var finite = Pattern.compile("request_seconds_bucket\\{route=\"/orders\",le=\"([^\"]+)\"\\} ([^ ]+)")
                .matcher(buckets.getFirst());
        assertTrue(finite.matches(), buckets.getFirst());
        assertNumber("0.001", finite.group(1));
        assertNumber("1", finite.group(2));
        assertSample(List.of(buckets.getLast()), "request_seconds_bucket{route=\"/orders\",le=\"+Inf\"}", "1");
        assertEquals('\n', scrape.charAt(scrape.length() - 1));
    }

    @Test
    void listenerSeesOnlyNewMetricsAndCanReadTheRegistry() {
        var events = new ArrayList<MetricId>();
        var tags = Map.of("route", "/orders");
        registry.gauge("queue_size", Map.of());

        try (var registration = registry.onRegistration((id, type) -> {
            registry.counter(id.name(), id.tags());
            events.add(id);
        })) {
            registry.counter("requests_total", tags);
            registry.counter("requests_total", tags);
        }
        registry.counter("requests_total", Map.of("route", "/cart"));
        assertEquals(List.of(new MetricId("requests_total", tags)), events);
    }

    private static void assertSample(List<String> lines, String key, String expected) {
        String prefix = key + " ";
        String sample = lines.stream()
                .filter(line -> line.startsWith(prefix))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing sample: " + key));
        assertNumber(expected, sample.substring(prefix.length()));
    }

    private static void assertNumber(String expected, String actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(new BigDecimal(actual)),
                () -> "Expected " + expected + ", got " + actual);
    }
}
