package com.coldchain.web;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSetMetaData;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Database-backed reports with date and text filters, pagination and CSV export. */
@Controller
@RequestMapping("/reports")
public class ReportController {
    private static final int PAGE = 20;
    record Def(String title, String sql, String dateCol, String search, Set<Role> roles, String order) {}
    public static class Table { public List<String> headers = new ArrayList<>(); public List<List<Object>> rows = new ArrayList<>(); }
    private static final Map<String, Def> DEFS = new LinkedHashMap<>();
    static {
        Set<Role> ops = EnumSet.of(Role.ADMIN, Role.WAREHOUSE_MANAGER, Role.TRANSPORT_MANAGER, Role.QUALITY_INSPECTOR);
        DEFS.put("inventory", new Def("Inventory and batch expiry", "SELECT p.code, p.name AS product, b.batch_no, b.quantity_on_hand AS on_hand, b.reserved, b.expiry_date, b.storage_location "
            + "FROM batches b JOIN products p ON p.id = b.product_id WHERE 1=1", "b.expiry_date", "lower(p.code || ' ' || p.name || ' ' || b.batch_no)",
            EnumSet.of(Role.ADMIN, Role.WAREHOUSE_MANAGER, Role.QUALITY_INSPECTOR), "b.expiry_date"));
        DEFS.put("shipments", new Def("Shipments by date and status", "SELECT s.tracking_no, p.name AS product, s.status, s.origin, s.destination, s.quantity, s.created_at "
            + "FROM shipments s JOIN products p ON p.id = s.product_id WHERE 1=1", "s.created_at", "lower(s.tracking_no || ' ' || p.name || ' ' || s.destination)",
            EnumSet.of(Role.ADMIN, Role.WAREHOUSE_MANAGER, Role.TRANSPORT_MANAGER), "s.id DESC"));
        DEFS.put("performance", new Def("Delivery performance", "SELECT s.tracking_no, s.expected_delivery, s.delivered_at, (s.delivered_at <= s.expected_delivery) AS on_time, s.quantity, s.received_qty, s.discrepancy "
            + "FROM shipments s WHERE s.status = 'DELIVERED'", "s.delivered_at", "lower(s.tracking_no)", EnumSet.of(Role.ADMIN, Role.TRANSPORT_MANAGER), "s.delivered_at DESC"));
        DEFS.put("excursions", new Def("Temperature and humidity excursions", "SELECT s.tracking_no, al.type, al.severity, al.reading_value, al.status, al.created_at "
            + "FROM alerts al JOIN shipments s ON s.id = al.shipment_id WHERE al.type IN ('HIGH_TEMP','LOW_TEMP','HIGH_HUMIDITY','LOW_HUMIDITY')", "al.created_at",
            "lower(s.tracking_no || ' ' || al.type)", ops, "al.id DESC"));
        DEFS.put("alerts", new Def("Alerts and quality investigations (active and resolved)", "SELECT al.id, s.tracking_no, al.type, al.severity, al.status, al.message, al.created_at, al.resolution_note, al.investigation_notes "
            + "FROM alerts al LEFT JOIN shipments s ON s.id = al.shipment_id WHERE 1=1", "al.created_at", "lower(al.type || ' ' || al.message)", ops, "al.id DESC"));
        DEFS.put("audit", new Def("Audit log", "SELECT a.created_at, u.name AS actor, a.action, a.detail FROM audit_logs a LEFT JOIN users u ON u.id = a.actor_id WHERE 1=1",
            "a.created_at", "lower(a.action || ' ' || coalesce(a.detail, ''))", EnumSet.of(Role.ADMIN), "a.id DESC"));
    }
    private final JdbcTemplate jdbc;

    public ReportController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static LocalDate date(String s) { try { return s == null || s.isBlank() ? null : LocalDate.parse(s); } catch (Exception e) { return null; } }

    private Def def(String type, AppPrincipal me) {
        Def d = DEFS.get(type);
        if (d == null || !d.roles().contains(me.role())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return d;
    }
    private String where(Def d, String q, LocalDate from, LocalDate to, List<Object> a) {
        StringBuilder s = new StringBuilder(d.sql());
        if (from != null) { s.append(" AND ").append(d.dateCol()).append(" >= ?"); a.add(java.sql.Date.valueOf(from)); }
        if (to != null) { s.append(" AND ").append(d.dateCol()).append(" < ?"); a.add(java.sql.Date.valueOf(to.plusDays(1))); }
        if (q != null && !q.isBlank()) { s.append(" AND ").append(d.search()).append(" LIKE ?"); a.add("%" + q.trim().toLowerCase() + "%"); }
        return s.toString();
    }
    private Table run(String sql, List<Object> args) {
        return jdbc.query(sql, (ResultSetExtractor<Table>) rs -> {
            Table t = new Table();
            ResultSetMetaData md = rs.getMetaData();
            for (int i = 1; i <= md.getColumnCount(); i++) t.headers.add(md.getColumnLabel(i));
            while (rs.next()) { List<Object> r = new ArrayList<>(); for (int i = 1; i <= md.getColumnCount(); i++) r.add(rs.getObject(i)); t.rows.add(r); }
            return t;
        }, args.toArray());
    }

    @GetMapping
    public String report(@RequestParam(required = false) String type, @RequestParam(required = false) String from, @RequestParam(required = false) String to,
                         @RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page, Model m, @AuthenticationPrincipal AppPrincipal me) {
        Map<String, String> types = new LinkedHashMap<>();
        DEFS.forEach((k, v) -> { if (v.roles().contains(me.role())) types.put(k, v.title()); });
        String t = type != null && types.containsKey(type) ? type : types.keySet().iterator().next();
        Def d = def(t, me);
        List<Object> a = new ArrayList<>();
        String sql = where(d, q, date(from), date(to), a);
        Integer total = jdbc.queryForObject("SELECT count(*) FROM (" + sql + ") x", Integer.class, a.toArray());
        int pg = Math.max(0, page);
        List<Object> a2 = new ArrayList<>(a); a2.add(PAGE); a2.add(pg * PAGE);
        m.addAttribute("table", run(sql + " ORDER BY " + d.order() + " LIMIT ? OFFSET ?", a2));
        m.addAttribute("types", types); m.addAttribute("type", t); m.addAttribute("title", d.title());
        m.addAttribute("total", total); m.addAttribute("page", pg); m.addAttribute("pages", Math.max(1, ((total == null ? 0 : total) + PAGE - 1) / PAGE));
        m.addAttribute("from", from); m.addAttribute("to", to); m.addAttribute("q", q);
        return "reports";
    }

    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> export(@RequestParam String type, @RequestParam(required = false) String from, @RequestParam(required = false) String to,
                                         @RequestParam(required = false) String q, @AuthenticationPrincipal AppPrincipal me) {
        Def d = def(type, me);
        List<Object> a = new ArrayList<>();
        String sql = where(d, q, date(from), date(to), a) + " ORDER BY " + d.order() + " LIMIT 10000";
        Table t = run(sql, a);
        StringBuilder sb = new StringBuilder(CsvUtil.row(t.headers)).append("\r\n");
        for (List<Object> r : t.rows) sb.append(CsvUtil.row(r)).append("\r\n");
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + type + ".csv\"")
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body(sb.toString().getBytes(StandardCharsets.UTF_8));
    }
}
