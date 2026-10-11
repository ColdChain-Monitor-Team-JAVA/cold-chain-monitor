package com.coldchain.web;

import java.sql.ResultSetMetaData;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** One dashboard per role (/dashboard/admin, /warehouse, /transport, /driver, /hospital, /supplier, /inspector), each with its own data and actions. */
@Controller
public class DashboardController {
    public static class Grid { public String title; public List<String> headers = new ArrayList<>(); public List<List<Object>> rows = new ArrayList<>(); }

    private final JdbcTemplate jdbc;
    public DashboardController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public static String slug(Role r) {
        return switch (r) {
            case ADMIN -> "admin"; case WAREHOUSE_MANAGER -> "warehouse"; case TRANSPORT_MANAGER -> "transport"; case DRIVER -> "driver";
            case HOSPITAL_STAFF -> "hospital"; case SUPPLIER -> "supplier"; case QUALITY_INSPECTOR -> "inspector";
        };
    }

    private int n(String sql, Object... a) { Integer v = jdbc.queryForObject(sql, Integer.class, a); return v == null ? 0 : v; }
    private static String[] card(String label, Object value) { return new String[]{label, String.valueOf(value), ""}; }
    private static String[] crit(String label, Object value) { return new String[]{label, String.valueOf(value), "crit"}; }
    private static String[] link(String label, String url) { return new String[]{label, url}; }

    private Grid grid(String title, String sql, Object... a) {
        Grid g = new Grid();
        g.title = title;
        jdbc.query(sql, (ResultSetExtractor<Void>) rs -> {
            ResultSetMetaData md = rs.getMetaData();
            for (int i = 1; i <= md.getColumnCount(); i++) g.headers.add(md.getColumnLabel(i));
            while (rs.next()) { List<Object> r = new ArrayList<>(); for (int i = 1; i <= md.getColumnCount(); i++) r.add(rs.getObject(i)); g.rows.add(r); }
            return null;
        }, a);
        return g;
    }

    private static String latest(String col) {
        return "(SELECT r." + col + " FROM sensor_readings r WHERE r.shipment_id = s.id ORDER BY r.recorded_at DESC, r.id DESC LIMIT 1)";
    }
    /** Shipment table with the latest SIMULATED reading and position. */
    private static String ship(String where) {
        return "SELECT s.tracking_no AS \"Tracking\", p.name AS \"Product\", s.origin || ' to ' || s.destination AS \"Route\", s.status AS \"Status\", "
            + latest("temperature") + " AS \"Temp °C\", " + latest("humidity") + " AS \"Humidity %\", "
            + "(SELECT round(r.latitude::numeric, 3) || ', ' || round(r.longitude::numeric, 3) FROM sensor_readings r WHERE r.shipment_id = s.id ORDER BY r.recorded_at DESC, r.id DESC LIMIT 1) AS \"Position (simulated)\", "
            + "to_char(s.expected_delivery, 'DD Mon HH24:MI') AS \"Expected\" FROM shipments s JOIN products p ON p.id = s.product_id WHERE " + where + " ORDER BY s.id DESC LIMIT 10";
    }
    private static final String OPEN_SHIP_ALERTS = "SELECT count(*) FROM alerts al JOIN shipments s ON s.id = al.shipment_id JOIN products p ON p.id = s.product_id WHERE al.status <> 'RESOLVED'";

    private String page(Model m, Role r, String title, String tip, List<String[]> cards, List<Grid> grids, List<String[]> actions) {
        m.addAttribute("slug", slug(r)); m.addAttribute("title", title); m.addAttribute("tip", tip);
        m.addAttribute("cards", cards); m.addAttribute("grids", grids); m.addAttribute("actions", actions);
        return "dashboard";
    }

    @GetMapping("/dashboard")
    public String home(@AuthenticationPrincipal AppPrincipal me) { return "redirect:/dashboard/" + slug(me.role()); }

    @GetMapping("/dashboard/admin")
    public String admin(Model m) {
        return page(m, Role.ADMIN, "Administrator dashboard", "System-wide overview, user administration and audit trail.",
            List.of(card("Users", n("SELECT count(*) FROM users")), card("Inactive users", n("SELECT count(*) FROM users WHERE NOT active")),
                card("Shipments", n("SELECT count(*) FROM shipments")), card("In transit", n("SELECT count(*) FROM shipments WHERE status = 'IN_TRANSIT'")),
                card("Open alerts", n("SELECT count(*) FROM alerts WHERE status <> 'RESOLVED'")), crit("Critical alerts", n("SELECT count(*) FROM alerts WHERE status <> 'RESOLVED' AND severity = 'CRITICAL'"))),
            List.of(grid("Users by role", "SELECT role AS \"Role\", count(*) AS \"Users\", count(*) FILTER (WHERE active) AS \"Active\" FROM users GROUP BY role ORDER BY role"),
                grid("Recent audit events", "SELECT to_char(a.created_at, 'DD Mon HH24:MI') AS \"Time\", coalesce(u.name, '-') AS \"By\", a.action AS \"Action\", coalesce(a.detail, '') AS \"Detail\" FROM audit_logs a LEFT JOIN users u ON u.id = a.actor_id ORDER BY a.id DESC LIMIT 8")),
            List.of(link("Manage users", "/users"), link("Reports", "/reports"), link("Alerts", "/alerts")));
    }

    @GetMapping("/dashboard/warehouse")
    public String warehouse(Model m) {
        return page(m, Role.WAREHOUSE_MANAGER, "Warehouse dashboard", "Stored goods, batch expiry and stock levels.",
            List.of(card("Batches", n("SELECT count(*) FROM batches")),
                crit("Low stock", n("SELECT count(*) FROM batches b JOIN products p ON p.id = b.product_id WHERE b.expiry_date >= current_date AND b.quantity_on_hand - b.reserved <= p.reorder_level")),
                crit("Expired with stock", n("SELECT count(*) FROM batches WHERE expiry_date < current_date AND quantity_on_hand > 0")),
                card("Expiring in 30 days", n("SELECT count(*) FROM batches WHERE expiry_date BETWEEN current_date AND current_date + 30")),
                card("Units reserved", n("SELECT coalesce(sum(reserved), 0) FROM batches")), card("Awaiting dispatch", n("SELECT count(*) FROM shipments WHERE status = 'CREATED'"))),
            List.of(grid("Batches needing attention", "SELECT p.code AS \"Code\", p.name AS \"Product\", b.batch_no AS \"Batch\", b.quantity_on_hand - b.reserved AS \"Available\", b.expiry_date AS \"Expiry\", b.storage_location AS \"Location\" "
                + "FROM batches b JOIN products p ON p.id = b.product_id WHERE b.expiry_date <= current_date + 30 OR b.quantity_on_hand - b.reserved <= p.reorder_level ORDER BY b.expiry_date LIMIT 10"),
                grid("Shipments awaiting dispatch", ship("s.status = 'CREATED'"))),
            List.of(link("Inventory", "/inventory"), link("New shipment", "/shipments/new"), link("Shipments", "/shipments")));
    }

    @GetMapping("/dashboard/transport")
    public String transport(Model m) {
        return page(m, Role.TRANSPORT_MANAGER, "Transport dashboard", "Vehicles on the road, delays, assignments and shipment alerts.",
            List.of(card("In transit", n("SELECT count(*) FROM shipments WHERE status = 'IN_TRANSIT'")), crit("Delayed", n("SELECT count(*) FROM shipments WHERE status = 'DELAYED'")),
                card("Awaiting dispatch", n("SELECT count(*) FROM shipments WHERE status = 'CREATED'")),
                card("No driver assigned", n("SELECT count(*) FROM shipments WHERE driver_id IS NULL AND status IN ('CREATED','IN_TRANSIT','DELAYED')")),
                card("Open shipment alerts", n("SELECT count(*) FROM alerts WHERE shipment_id IS NOT NULL AND status <> 'RESOLVED'")), card("Delivered", n("SELECT count(*) FROM shipments WHERE status = 'DELIVERED'"))),
            List.of(grid("On the road", ship("s.status IN ('IN_TRANSIT','DELAYED')")), grid("Awaiting dispatch", ship("s.status = 'CREATED'")),
                grid("Open shipment alerts", "SELECT s.tracking_no AS \"Tracking\", al.type AS \"Type\", al.severity AS \"Severity\", al.status AS \"Status\", al.message AS \"Details\" "
                    + "FROM alerts al JOIN shipments s ON s.id = al.shipment_id WHERE al.status <> 'RESOLVED' ORDER BY al.id DESC LIMIT 8")),
            List.of(link("New shipment", "/shipments/new"), link("Shipments", "/shipments"), link("Reports", "/reports")));
    }

    @GetMapping("/dashboard/driver")
    public String driver(Model m, @AuthenticationPrincipal AppPrincipal me) {
        long id = me.id();
        return page(m, Role.DRIVER, "Driver dashboard - welcome, " + me.displayName(), "Only shipments assigned to you are shown.",
            List.of(card("Assigned (open)", n("SELECT count(*) FROM shipments WHERE driver_id = ? AND status IN ('CREATED','IN_TRANSIT','DELAYED')", id)),
                card("In transit", n("SELECT count(*) FROM shipments WHERE driver_id = ? AND status = 'IN_TRANSIT'", id)),
                card("Delivered", n("SELECT count(*) FROM shipments WHERE driver_id = ? AND status = 'DELIVERED'", id)),
                crit("Open alerts on my shipments", n(OPEN_SHIP_ALERTS + " AND s.driver_id = ?", id))),
            List.of(grid("My active shipments (open one to update status or confirm delivery)", ship("s.driver_id = ? AND s.status IN ('CREATED','IN_TRANSIT','DELAYED')"), id)),
            List.of(link("My shipments", "/shipments"), link("Alerts", "/alerts")));
    }

    @GetMapping("/dashboard/hospital")
    public String hospital(Model m, @AuthenticationPrincipal AppPrincipal me) {
        long id = me.id();
        return page(m, Role.HOSPITAL_STAFF, "Hospital dashboard", "Incoming shipments addressed to you, their condition, and receipts.",
            List.of(card("Incoming", n("SELECT count(*) FROM shipments WHERE hospital_id = ? AND status IN ('CREATED','IN_TRANSIT','DELAYED')", id)),
                crit("Delayed", n("SELECT count(*) FROM shipments WHERE hospital_id = ? AND status = 'DELAYED'", id)),
                card("Received", n("SELECT count(*) FROM shipments WHERE hospital_id = ? AND status = 'DELIVERED'", id)),
                crit("Received with discrepancy", n("SELECT count(*) FROM shipments WHERE hospital_id = ? AND discrepancy", id)),
                crit("Open alerts on my shipments", n(OPEN_SHIP_ALERTS + " AND s.hospital_id = ?", id))),
            List.of(grid("Incoming shipments (open one to confirm receipt)", ship("s.hospital_id = ? AND s.status IN ('CREATED','IN_TRANSIT','DELAYED')"), id),
                grid("Recently received", "SELECT s.tracking_no AS \"Tracking\", p.name AS \"Product\", s.quantity AS \"Sent\", s.received_qty AS \"Received\", CASE WHEN s.discrepancy THEN 'DISCREPANCY' ELSE 'OK' END AS \"Result\", "
                    + "to_char(s.delivered_at, 'DD Mon HH24:MI') AS \"Delivered\" FROM shipments s JOIN products p ON p.id = s.product_id WHERE s.hospital_id = ? AND s.status = 'DELIVERED' ORDER BY s.delivered_at DESC LIMIT 8", id)),
            List.of(link("Incoming shipments", "/shipments"), link("Alerts", "/alerts")));
    }

    @GetMapping("/dashboard/supplier")
    public String supplier(Model m, @AuthenticationPrincipal AppPrincipal me) {
        long id = me.id();
        return page(m, Role.SUPPLIER, "Supplier dashboard", "Your products, stock and the shipments carrying them.",
            List.of(card("My products", n("SELECT count(*) FROM products WHERE supplier_id = ?", id)),
                card("Units on hand", n("SELECT coalesce(sum(b.quantity_on_hand), 0) FROM batches b JOIN products p ON p.id = b.product_id WHERE p.supplier_id = ?", id)),
                card("Units reserved", n("SELECT coalesce(sum(b.reserved), 0) FROM batches b JOIN products p ON p.id = b.product_id WHERE p.supplier_id = ?", id)),
                card("Active shipments", n("SELECT count(*) FROM shipments s JOIN products p ON p.id = s.product_id WHERE p.supplier_id = ? AND s.status IN ('CREATED','IN_TRANSIT','DELAYED')", id)),
                crit("Open alerts on my shipments", n(OPEN_SHIP_ALERTS + " AND p.supplier_id = ?", id))),
            List.of(grid("My stock", "SELECT p.code AS \"Code\", p.name AS \"Product\", b.batch_no AS \"Batch\", b.quantity_on_hand AS \"On hand\", b.reserved AS \"Reserved\", b.expiry_date AS \"Expiry\" "
                + "FROM batches b JOIN products p ON p.id = b.product_id WHERE p.supplier_id = ? ORDER BY b.expiry_date LIMIT 10", id),
                grid("Shipments of my products", ship("p.supplier_id = ?"), id)),
            List.of(link("Inventory", "/inventory"), link("Shipments", "/shipments")));
    }

    @GetMapping("/dashboard/inspector")
    public String inspector(Model m) {
        int compliance = n("SELECT CASE WHEN count(*) = 0 THEN 100 ELSE round(100.0 * count(*) FILTER (WHERE r.temperature BETWEEN p.min_temp AND p.max_temp AND r.humidity BETWEEN p.min_humidity AND p.max_humidity) / count(*)) END "
            + "FROM sensor_readings r JOIN shipments s ON s.id = r.shipment_id JOIN products p ON p.id = s.product_id WHERE r.recorded_at > now() - interval '24 hours'");
        return page(m, Role.QUALITY_INSPECTOR, "Quality inspector dashboard", "Excursions, compliance and alerts awaiting investigation.",
            List.of(crit("Critical open alerts", n("SELECT count(*) FROM alerts WHERE status <> 'RESOLVED' AND severity = 'CRITICAL'")),
                card("Not yet acknowledged", n("SELECT count(*) FROM alerts WHERE status = 'OPEN'")),
                card("Excursions (24h)", n("SELECT count(*) FROM alerts WHERE type IN ('HIGH_TEMP','LOW_TEMP','HIGH_HUMIDITY','LOW_HUMIDITY') AND created_at > now() - interval '24 hours'")),
                card("Readings in range (24h)", compliance + "%"), crit("Expired batches with stock", n("SELECT count(*) FROM batches WHERE expiry_date < current_date AND quantity_on_hand > 0"))),
            List.of(grid("Alerts awaiting investigation", "SELECT al.id AS \"Alert\", coalesce(s.tracking_no, 'Inventory') AS \"Source\", al.type AS \"Type\", al.severity AS \"Severity\", al.status AS \"Status\", al.message AS \"Details\" "
                + "FROM alerts al LEFT JOIN shipments s ON s.id = al.shipment_id WHERE al.status <> 'RESOLVED' ORDER BY (al.severity = 'CRITICAL') DESC, al.id DESC LIMIT 10"),
                grid("Expired batches", "SELECT p.name AS \"Product\", b.batch_no AS \"Batch\", b.quantity_on_hand AS \"Units\", b.expiry_date AS \"Expired on\" FROM batches b JOIN products p ON p.id = b.product_id "
                    + "WHERE b.expiry_date < current_date AND b.quantity_on_hand > 0 ORDER BY b.expiry_date LIMIT 8")),
            List.of(link("Alerts and investigations", "/alerts"), link("Reports", "/reports"), link("Inventory", "/inventory")));
    }
}
