package com.coldchain.web;

import java.util.Map;
import java.util.Set;

/** Pure business rules: product-specific threshold evaluation and shipment status transitions. */
public final class Rules {
    private Rules() {}

    public record Breach(String direction, String severity) {}

    /** Returns null when within [min, max]; CRITICAL when outside by more than margin, otherwise WARNING. */
    public static Breach evaluate(double value, double min, double max, double margin) {
        if (value >= min && value <= max) return null;
        boolean high = value > max;
        double over = high ? value - max : min - value;
        return new Breach(high ? "HIGH" : "LOW", over > margin ? "CRITICAL" : "WARNING");
    }

    private static final Map<String, Set<String>> ALLOWED = Map.of(
        "CREATED", Set.of("IN_TRANSIT", "CANCELLED"), "IN_TRANSIT", Set.of("DELIVERED", "DELAYED"),
        "DELAYED", Set.of("IN_TRANSIT", "DELIVERED", "CANCELLED"), "DELIVERED", Set.of(), "CANCELLED", Set.of());

    /** Inventory exception status for a batch. */
    public static String stockStatus(int available, int reorderLevel, java.time.LocalDate expiry, java.time.LocalDate today) {
        if (expiry.isBefore(today)) return "EXPIRED";
        if (available <= reorderLevel) return "LOW";
        if (!expiry.isAfter(today.plusDays(30))) return "EXPIRING";
        return "OK";
    }

    public static Set<String> next(String status) { return ALLOWED.getOrDefault(status, Set.of()); }
    public static boolean allowed(String from, String to) { return next(from).contains(to); }
}
