package com.coldchain.web;

import java.util.List;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** SOFTWARE SIMULATION of IoT temperature/humidity sensors and GPS. Scheduler lifecycle is managed by Spring. */
@Service
public class MonitoringService {
    private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final boolean enabled;
    private final long staleSeconds;
    private final Random rnd = new Random();

    public MonitoringService(JdbcTemplate jdbc, TransactionTemplate tx, @Value("${coldchain.simulation.enabled}") boolean enabled,
                             @Value("${coldchain.simulation.stale-seconds}") long staleSeconds) {
        this.jdbc = jdbc; this.tx = tx; this.enabled = enabled; this.staleSeconds = staleSeconds;
    }

    @Scheduled(fixedDelayString = "${coldchain.simulation.interval-ms}", initialDelay = 5000)
    public void tick() {
        if (!enabled) return;
        try { tx.executeWithoutResult(s -> { checkExceptions(); simulate(); }); } catch (Exception e) { log.warn("Simulation tick failed: {}", e.getMessage()); }
    }

    private static double num(Map<String, Object> r, String k) { return ((Number) r.get(k)).doubleValue(); }

    void simulate() {
        List<Map<String, Object>> ships = jdbc.queryForList(
            "SELECT s.id, s.start_lat, s.start_lon, s.dest_lat, s.dest_lon, p.min_temp, p.max_temp, p.min_humidity, p.max_humidity "
            + "FROM shipments s JOIN products p ON p.id = s.product_id WHERE s.status IN ('IN_TRANSIT', 'DELAYED')");
        for (Map<String, Object> r : ships) {
            long id = ((Number) r.get("id")).longValue();
            double minT = num(r, "min_temp"), maxT = num(r, "max_temp"), minH = num(r, "min_humidity"), maxH = num(r, "max_humidity");
            List<Map<String, Object>> last = jdbc.queryForList(
                "SELECT temperature, humidity, latitude, longitude FROM sensor_readings WHERE shipment_id = ? ORDER BY recorded_at DESC, id DESC LIMIT 1", id);
            double midT = (minT + maxT) / 2, midH = (minH + maxH) / 2;
            double t = last.isEmpty() ? midT : num(last.get(0), "temperature");
            double h = last.isEmpty() ? midH : num(last.get(0), "humidity");
            t += (midT - t) * 0.15 + (rnd.nextDouble() - 0.5);
            h += (midH - h) * 0.15 + (rnd.nextDouble() - 0.5) * 2;
            if (rnd.nextDouble() < 0.06) t = rnd.nextBoolean() ? maxT + 0.5 + rnd.nextDouble() * 3 : minT - 0.5 - rnd.nextDouble() * 3;
            if (rnd.nextDouble() < 0.03) h = rnd.nextBoolean() ? maxH + 1 + rnd.nextDouble() * 15 : minH - 1 - rnd.nextDouble() * 15;
            double lat = last.isEmpty() ? num(r, "start_lat") : num(last.get(0), "latitude");
            double lon = last.isEmpty() ? num(r, "start_lon") : num(last.get(0), "longitude");
            lat += (num(r, "dest_lat") - lat) * 0.03;
            lon += (num(r, "dest_lon") - lon) * 0.03;
            jdbc.update("INSERT INTO sensor_readings(shipment_id,sensor_id,temperature,humidity,latitude,longitude) VALUES (?,?,?,?,?,?)",
                id, "SIM-" + id, t, h, lat, lon);
            Rules.Breach bt = Rules.evaluate(t, minT, maxT, 2.0);
            if (bt != null) raise(id, bt.direction().equals("HIGH") ? "HIGH_TEMP" : "LOW_TEMP", bt.severity(),
                String.format("Simulated temperature %.1f °C is outside %.1f to %.1f °C", t, minT, maxT), t);
            Rules.Breach bh = Rules.evaluate(h, minH, maxH, 10.0);
            if (bh != null) raise(id, bh.direction().equals("HIGH") ? "HIGH_HUMIDITY" : "LOW_HUMIDITY", bh.severity(),
                String.format("Simulated humidity %.1f %% is outside %.1f to %.1f %%", h, minH, maxH), h);
        }
    }

    /** Delayed shipments, stale sensors, expired and low-stock batches. Each alert type is deduplicated by a partial unique index. */
    void checkExceptions() {
        for (Long id : jdbc.queryForList("UPDATE shipments SET status = 'DELAYED' WHERE status = 'IN_TRANSIT' AND expected_delivery IS NOT NULL AND expected_delivery < now() RETURNING id", Long.class)) {
            jdbc.update("INSERT INTO shipment_status_history(shipment_id,from_status,to_status,note) VALUES (?,'IN_TRANSIT','DELAYED','Automatic: past expected delivery time')", id);
            raise(id, "SHIPMENT_DELAYED", "WARNING", "Shipment passed its expected delivery time", null);
        }
        for (Long id : jdbc.queryForList("SELECT s.id FROM shipments s WHERE s.status IN ('IN_TRANSIT','DELAYED') AND "
            + "(SELECT max(r.recorded_at) FROM sensor_readings r WHERE r.shipment_id = s.id) < now() - make_interval(secs => ?)", Long.class, (double) staleSeconds)) {
            raise(id, "SENSOR_STALE", "WARNING", "No simulated sensor reading received for over " + staleSeconds + " seconds", null);
        }
        jdbc.update("INSERT INTO alerts(batch_id,type,severity,message) SELECT b.id,'BATCH_EXPIRED','CRITICAL','Batch ' || b.batch_no || ' expired on ' || b.expiry_date "
            + "FROM batches b WHERE b.expiry_date < current_date AND b.quantity_on_hand > 0 ON CONFLICT DO NOTHING");
        jdbc.update("INSERT INTO alerts(batch_id,type,severity,message) SELECT b.id,'LOW_STOCK','WARNING','Batch ' || b.batch_no || ' of ' || p.name || ' is at or below its reorder level' "
            + "FROM batches b JOIN products p ON p.id = b.product_id WHERE b.expiry_date >= current_date AND b.quantity_on_hand - b.reserved <= p.reorder_level ON CONFLICT DO NOTHING");
    }

    /** Duplicate policy: the partial unique index allows one unresolved alert per shipment and type. */
    private void raise(long shipmentId, String type, String severity, String message, Double value) {
        jdbc.update("INSERT INTO alerts(shipment_id,type,severity,message,reading_value) VALUES (?,?,?,?,?) ON CONFLICT DO NOTHING",
            shipmentId, type, severity, message, value);
    }
}
