package edu.hse.concurrency.metrics;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public record MetricId(String name, Map<String, String> tags) {

    private static final Pattern IDENTIFIER = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

    public MetricId {
        Objects.requireNonNull(name, "name");
        tags = Map.copyOf(tags);
        if (!IDENTIFIER.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid metric name: " + name);
        }
        for (String key : tags.keySet()) {
            if (!IDENTIFIER.matcher(key).matches() || key.startsWith("__") || key.equals("le")) {
                throw new IllegalArgumentException("Invalid tag name: " + key);
            }
        }
    }
}
