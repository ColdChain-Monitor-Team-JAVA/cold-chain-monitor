package com.coldchain.web;

import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AppController {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder enc;
    private static final int PAGE = 10;

    public AppController(JdbcTemplate jdbc, PasswordEncoder enc) { this.jdbc = jdbc; this.enc = enc; }

    @ModelAttribute
    public void common(Model m, @AuthenticationPrincipal AppPrincipal me) {
        if (me != null) { m.addAttribute("meName", me.displayName()); m.addAttribute("meRole", me.role().label()); }
    }

    // Server-side data scoping: drivers see their shipments, suppliers see their products' shipments.
    private static String scope(Role r) {
        return switch (r) { case DRIVER -> " AND s.driver_id = ?"; case SUPPLIER -> " AND p.supplier_id = ?"; case HOSPITAL_STAFF -> " AND s.hospital_id = ?"; default -> ""; };
    }
    private static Object[] scopeArgs(AppPrincipal me) {
        return EnumSet.of(Role.DRIVER, Role.SUPPLIER, Role.HOSPITAL_STAFF).contains(me.role()) ? new Object[]{me.id()} : new Object[0];
    }
    private static Object[] join(Object[] a, Object... b) {
        Object[] r = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }
    private void audit(long actor, String action, String detail) {
        jdbc.update("INSERT INTO audit_logs(actor_id,action,detail) VALUES (?,?,?)", actor, action, detail);
    }

    @GetMapping("/")
    public String root() { return "redirect:/dashboard"; }

    @GetMapping("/shipments")
    public String shipments(@RequestParam(required = false) String q, @RequestParam(required = false) String status,
                            @RequestParam(defaultValue = "0") int page, Model m, @AuthenticationPrincipal AppPrincipal me) {
        StringBuilder w = new StringBuilder(" FROM shipments s JOIN products p ON p.id = s.product_id LEFT JOIN users d ON d.id = s.driver_id WHERE 1=1" + scope(me.role()));
        List<Object> args = new ArrayList<>(Arrays.asList(scopeArgs(me)));
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase() + "%";
            w.append(" AND (lower(s.tracking_no) LIKE ? OR lower(p.name) LIKE ? OR lower(s.destination) LIKE ?)");
            args.add(like); args.add(like); args.add(like);
        }
        if (status != null && !status.isBlank()) { w.append(" AND s.status = ?"); args.add(status); }
        Integer total = jdbc.queryForObject("SELECT count(*)" + w, Integer.class, args.toArray());
        int pg = Math.max(0, page);
        args.add(PAGE); args.add(pg * PAGE);
        m.addAttribute("rows", jdbc.queryForList("SELECT s.id, s.tracking_no, s.origin, s.destination, s.status, s.quantity, p.name AS product, d.name AS driver"
            + w + " ORDER BY s.id DESC LIMIT ? OFFSET ?", args.toArray()));
        m.addAttribute("page", pg); m.addAttribute("pages", Math.max(1, (total == null ? 0 : total + PAGE - 1) / PAGE));
        m.addAttribute("q", q); m.addAttribute("status", status);
        m.addAttribute("statuses", List.of("CREATED", "IN_TRANSIT", "DELAYED", "DELIVERED", "CANCELLED"));
        m.addAttribute("canCreate", EnumSet.of(Role.ADMIN, Role.TRANSPORT_MANAGER, Role.WAREHOUSE_MANAGER).contains(me.role()));
        return "shipments";
    }

    @GetMapping("/shipments/{id}")
    public String shipment(@PathVariable long id, Model m, @AuthenticationPrincipal AppPrincipal me) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT s.*, bt.batch_no, p.name AS product, p.code, p.min_temp, p.max_temp, p.min_humidity, p.max_humidity, d.name AS driver "
            + "FROM shipments s JOIN products p ON p.id = s.product_id LEFT JOIN users d ON d.id = s.driver_id LEFT JOIN batches bt ON bt.id = s.batch_id WHERE s.id = ?" + scope(me.role()),
            join(new Object[]{id}, scopeArgs(me)));
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        Map<String, Object> sh = rows.get(0);
        List<Map<String, Object>> rd = new ArrayList<>(jdbc.queryForList(
            "SELECT temperature, humidity, latitude, longitude, recorded_at, sensor_id FROM sensor_readings WHERE shipment_id = ? ORDER BY recorded_at DESC, id DESC LIMIT 60", id));
        m.addAttribute("readings", rd.subList(0, Math.min(rd.size(), 10)));
        Collections.reverse(rd);
        List<Double> temps = rd.stream().map(x -> ((Number) x.get("temperature")).doubleValue()).toList();
        List<Double> hums = rd.stream().map(x -> ((Number) x.get("humidity")).doubleValue()).toList();
        if (!temps.isEmpty()) m.addAttribute("tStats", temps.stream().mapToDouble(Double::doubleValue).summaryStatistics());
        if (!hums.isEmpty()) m.addAttribute("hStats", hums.stream().mapToDouble(Double::doubleValue).summaryStatistics());
        m.addAttribute("tempChart", chart(temps, ((Number) sh.get("min_temp")).doubleValue(), ((Number) sh.get("max_temp")).doubleValue()));
        m.addAttribute("humChart", chart(hums, ((Number) sh.get("min_humidity")).doubleValue(), ((Number) sh.get("max_humidity")).doubleValue()));
        if (!rd.isEmpty()) m.addAttribute("latest", rd.get(rd.size() - 1));
        Set<String> next = new TreeSet<>(Rules.next((String) sh.get("status")));
        next.remove("DELIVERED");
        boolean canUpdate = me.role() == Role.ADMIN || me.role() == Role.TRANSPORT_MANAGER || me.role() == Role.DRIVER;
        boolean canCancel = next.remove("CANCELLED") && me.role() != Role.DRIVER && canUpdate;
        String stt = (String) sh.get("status");
        boolean canAssign = (me.role() == Role.ADMIN || me.role() == Role.TRANSPORT_MANAGER) && !stt.equals("DELIVERED") && !stt.equals("CANCELLED");
        m.addAttribute("sh", sh); m.addAttribute("next", next); m.addAttribute("canUpdate", canUpdate); m.addAttribute("canCancel", canCancel);
        m.addAttribute("canDeliver", (stt.equals("IN_TRANSIT") || stt.equals("DELAYED"))
            && EnumSet.of(Role.ADMIN, Role.TRANSPORT_MANAGER, Role.DRIVER, Role.HOSPITAL_STAFF).contains(me.role()));
        m.addAttribute("canAssign", canAssign);
        if (canAssign) {
            m.addAttribute("drivers", jdbc.queryForList("SELECT id, name FROM users WHERE role = 'DRIVER' AND active ORDER BY name"));
            m.addAttribute("tms", jdbc.queryForList("SELECT id, name FROM users WHERE role = 'TRANSPORT_MANAGER' AND active ORDER BY name"));
        }
        m.addAttribute("history", jdbc.queryForList(
            "SELECT h.from_status, h.to_status, h.note, h.changed_at, u.name AS changed_by_name FROM shipment_status_history h "
            + "LEFT JOIN users u ON u.id = h.changed_by WHERE h.shipment_id = ? ORDER BY h.id DESC", id));
        m.addAttribute("alerts", jdbc.queryForList(
            "SELECT id, type, severity, status, message, created_at FROM alerts WHERE shipment_id = ? ORDER BY id DESC LIMIT 10", id));
        return "shipment";
    }

    @PostMapping("/shipments/{id}/status")
    @Transactional
    public String updateStatus(@PathVariable long id, @RequestParam String to, @RequestParam(required = false) String note,
                               @AuthenticationPrincipal AppPrincipal me, RedirectAttributes ra) {
        List<String> cur = jdbc.queryForList("SELECT s.status FROM shipments s JOIN products p ON p.id = s.product_id WHERE s.id = ?"
            + scope(me.role()) + " FOR UPDATE OF s", String.class, join(new Object[]{id}, scopeArgs(me)));
        if (cur.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        String from = cur.get(0);
        if (!Rules.allowed(from, to) || to.equals("DELIVERED") || (me.role() == Role.DRIVER && to.equals("CANCELLED"))) {
            ra.addFlashAttribute("err", "Invalid status change: " + from + " to " + to + ". (Deliveries are recorded with the delivery confirmation form.)");
            return "redirect:/shipments/" + id;
        }
        String n = note == null ? null : note.trim();
        if (n != null && n.length() > 200) n = n.substring(0, 200);
        jdbc.update("UPDATE shipments SET status = ? WHERE id = ?", to, id);
        if (to.equals("CANCELLED")) jdbc.update("UPDATE batches SET reserved = batches.reserved - s.quantity FROM shipments s WHERE s.id = ? AND batches.id = s.batch_id", id);
        jdbc.update("INSERT INTO shipment_status_history(shipment_id,from_status,to_status,changed_by,note) VALUES (?,?,?,?,?)", id, from, to, me.id(), n);
        audit(me.id(), "SHIPMENT_STATUS", "Shipment " + id + ": " + from + " -> " + to);
        ra.addFlashAttribute("msg", "Status updated to " + to + ".");
        return "redirect:/shipments/" + id;
    }

    @GetMapping("/alerts")
    public String alerts(@RequestParam(required = false) String status, Model m, @AuthenticationPrincipal AppPrincipal me) {
        StringBuilder sql = new StringBuilder(
            "SELECT al.id, al.type, al.severity, al.status, al.message, al.created_at, al.resolution_note, al.investigation_notes, s.tracking_no, s.id AS shipment_id "
            + "FROM alerts al LEFT JOIN shipments s ON s.id = al.shipment_id LEFT JOIN batches ab ON ab.id = al.batch_id LEFT JOIN products p ON p.id = COALESCE(s.product_id, ab.product_id) WHERE 1=1" + scope(me.role()));
        List<Object> args = new ArrayList<>(Arrays.asList(scopeArgs(me)));
        if (status != null && !status.isBlank()) { sql.append(" AND al.status = ?"); args.add(status); }
        sql.append(" ORDER BY al.id DESC LIMIT 50");
        m.addAttribute("rows", jdbc.queryForList(sql.toString(), args.toArray()));
        m.addAttribute("status", status);
        m.addAttribute("canAck", EnumSet.of(Role.ADMIN, Role.QUALITY_INSPECTOR, Role.TRANSPORT_MANAGER, Role.WAREHOUSE_MANAGER).contains(me.role()));
        m.addAttribute("canResolve", me.role() == Role.ADMIN || me.role() == Role.QUALITY_INSPECTOR);
        return "alerts";
    }

    @PostMapping("/alerts/{id}/acknowledge")
    public String acknowledge(@PathVariable long id, @AuthenticationPrincipal AppPrincipal me, RedirectAttributes ra) {
        int n = jdbc.update("UPDATE alerts SET status = 'ACKNOWLEDGED', acknowledged_by = ?, acknowledged_at = now() WHERE id = ? AND status = 'OPEN'", me.id(), id);
        if (n == 0) ra.addFlashAttribute("err", "Alert is not open."); else { audit(me.id(), "ALERT_ACK", "Alert " + id); ra.addFlashAttribute("msg", "Alert acknowledged."); }
        return "redirect:/alerts";
    }

    @PostMapping("/alerts/{id}/resolve")
    public String resolve(@PathVariable long id, @RequestParam String note, @AuthenticationPrincipal AppPrincipal me, RedirectAttributes ra) {
        String n = note.trim();
        if (n.isEmpty() || n.length() > 500) { ra.addFlashAttribute("err", "A resolution note (1-500 characters) is required."); return "redirect:/alerts"; }
        int c = jdbc.update("UPDATE alerts SET status = 'RESOLVED', resolved_by = ?, resolved_at = now(), resolution_note = ? WHERE id = ? AND status <> 'RESOLVED'", me.id(), n, id);
        if (c == 0) ra.addFlashAttribute("err", "Alert is already resolved."); else { audit(me.id(), "ALERT_RESOLVE", "Alert " + id); ra.addFlashAttribute("msg", "Alert resolved."); }
        return "redirect:/alerts";
    }

    @GetMapping("/users")
    public String users(@RequestParam(required = false) String q, Model m) {
        String like = "%" + (q == null ? "" : q.trim().toLowerCase()) + "%";
        m.addAttribute("rows", jdbc.queryForList("SELECT id, name, email, role, active, created_at FROM users WHERE lower(name) LIKE ? OR lower(email) LIKE ? ORDER BY id", like, like));
        m.addAttribute("roles", Role.values()); m.addAttribute("q", q);
        m.addAttribute("audit", jdbc.queryForList("SELECT a.action, a.detail, a.created_at, u.name AS actor FROM audit_logs a LEFT JOIN users u ON u.id = a.actor_id ORDER BY a.id DESC LIMIT 15"));
        return "users";
    }

    @PostMapping("/users")
    public String createUser(@RequestParam String name, @RequestParam String email, @RequestParam String role, @RequestParam String password,
                             @AuthenticationPrincipal AppPrincipal me, RedirectAttributes ra) {
        String nm = name.trim(), em = email.trim().toLowerCase();
        Role r;
        try { r = Role.valueOf(role); } catch (IllegalArgumentException e) { ra.addFlashAttribute("err", "Select a valid role."); return "redirect:/users"; }
        if (nm.isEmpty() || nm.length() > 100) ra.addFlashAttribute("err", "Name is required (max 100 characters).");
        else if (em.length() > 150 || !em.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) ra.addFlashAttribute("err", "Enter a valid email address.");
        else if (password.length() < 8) ra.addFlashAttribute("err", "Password must be at least 8 characters.");
        else {
            try {
                jdbc.update("INSERT INTO users(name,email,password_hash,role) VALUES (?,?,?,?)", nm, em, enc.encode(password), r.name());
                audit(me.id(), "USER_CREATED", em + " as " + r.label());
                ra.addFlashAttribute("msg", "User created.");
            } catch (DuplicateKeyException e) { ra.addFlashAttribute("err", "A user with this email already exists."); }
        }
        return "redirect:/users";
    }

    @PostMapping("/users/{id}/active")
    public String setActive(@PathVariable long id, @RequestParam boolean active, @AuthenticationPrincipal AppPrincipal me, RedirectAttributes ra) {
        if (id == me.id() && !active) { ra.addFlashAttribute("err", "You cannot deactivate the account you are currently using."); return "redirect:/users"; }
        if (jdbc.update("UPDATE users SET active = ? WHERE id = ?", active, id) > 0) {
            audit(me.id(), active ? "USER_ACTIVATED" : "USER_DEACTIVATED", "User " + id);
            ra.addFlashAttribute("msg", active ? "User activated." : "User deactivated.");
        }
        return "redirect:/users";
    }

    @PostMapping("/users/{id}/role")
    public String setRole(@PathVariable long id, @RequestParam String role, @AuthenticationPrincipal AppPrincipal me, RedirectAttributes ra) {
        Role r;
        try { r = Role.valueOf(role); } catch (IllegalArgumentException e) { ra.addFlashAttribute("err", "Select a valid role."); return "redirect:/users"; }
        if (id == me.id()) { ra.addFlashAttribute("err", "You cannot change the role of the account you are currently using."); return "redirect:/users"; }
        if (jdbc.update("UPDATE users SET role = ? WHERE id = ?", r.name(), id) > 0) { audit(me.id(), "USER_ROLE", "User " + id + " -> " + r.label()); ra.addFlashAttribute("msg", "Role updated."); }
        return "redirect:/users";
    }

    @PostMapping("/alerts/{id}/note")
    public String alertNote(@PathVariable long id, @RequestParam String note, @AuthenticationPrincipal AppPrincipal me, RedirectAttributes ra) {
        String n = note.trim();
        if (n.isEmpty() || n.length() > 500) { ra.addFlashAttribute("err", "An investigation note (1-500 characters) is required."); return "redirect:/alerts"; }
        String line = java.time.LocalDate.now() + " " + me.displayName() + ": " + n;
        int c = jdbc.update("UPDATE alerts SET investigation_notes = left(coalesce(investigation_notes || E'\\n', '') || ?, 2000) WHERE id = ?", line, id);
        if (c > 0) { audit(me.id(), "ALERT_NOTE", "Alert " + id); ra.addFlashAttribute("msg", "Investigation note added."); }
        return "redirect:/alerts";
    }

    @PostMapping("/users/{id}/edit")
    public String editUser(@PathVariable long id, @RequestParam String name, @RequestParam String email, @AuthenticationPrincipal AppPrincipal me, RedirectAttributes ra) {
        String nm = name.trim(), em = email.trim().toLowerCase();
        if (nm.isEmpty() || nm.length() > 100) ra.addFlashAttribute("err", "Name is required (max 100 characters).");
        else if (em.length() > 150 || !em.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) ra.addFlashAttribute("err", "Enter a valid email address.");
        else {
            try {
                if (jdbc.update("UPDATE users SET name = ?, email = ? WHERE id = ?", nm, em, id) > 0) { audit(me.id(), "USER_EDITED", "User " + id); ra.addFlashAttribute("msg", "User updated."); }
            } catch (DuplicateKeyException e) { ra.addFlashAttribute("err", "A user with this email already exists."); }
        }
        return "redirect:/users";
    }

    @PostMapping("/users/{id}/reset-password")
    public String resetPassword(@PathVariable long id, @AuthenticationPrincipal AppPrincipal me, RedirectAttributes ra) {
        if (id == me.id()) { ra.addFlashAttribute("err", "Ask another administrator to reset your own password."); return "redirect:/users"; }
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        java.security.SecureRandom rnd = new java.security.SecureRandom();
        StringBuilder tmp = new StringBuilder();
        for (int i = 0; i < 12; i++) tmp.append(chars.charAt(rnd.nextInt(chars.length())));
        if (jdbc.update("UPDATE users SET password_hash = ? WHERE id = ?", enc.encode(tmp), id) > 0) {
            audit(me.id(), "PASSWORD_RESET", "User " + id);
            ra.addFlashAttribute("msg", "Temporary password (shown once, share it securely): " + tmp);
        }
        return "redirect:/users";
    }

    private static Map<String, String> chart(List<Double> v, double min, double max) {
        if (v.size() < 2) return null;
        double lo = Math.min(min, v.stream().mapToDouble(d -> d).min().orElse(min)) - 1;
        double hi = Math.max(max, v.stream().mapToDouble(d -> d).max().orElse(max)) + 1;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < v.size(); i++) sb.append(String.format(Locale.ROOT, "%.1f,%.1f ", i * 600.0 / (v.size() - 1), y(v.get(i), lo, hi)));
        double top = y(max, lo, hi), bot = y(min, lo, hi);
        return Map.of("points", sb.toString().trim(), "bandY", String.format(Locale.ROOT, "%.1f", top), "bandH", String.format(Locale.ROOT, "%.1f", bot - top));
    }
    private static double y(double v, double lo, double hi) { return 160 - (v - lo) / (hi - lo) * 160; }
}
