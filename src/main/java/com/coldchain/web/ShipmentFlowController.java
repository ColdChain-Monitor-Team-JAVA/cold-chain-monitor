package com.coldchain.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Shipment creation with stock reservation, delivery confirmation with discrepancy handling, and assignments. */
@Controller
public class ShipmentFlowController {
    static final Map<String, double[]> HUBS = new LinkedHashMap<>();
    static {
        HUBS.put("Bengaluru", new double[]{12.97, 77.59}); HUBS.put("Chennai", new double[]{13.08, 80.27});
        HUBS.put("Mysuru", new double[]{12.30, 76.64}); HUBS.put("Hyderabad", new double[]{17.38, 78.48});
        HUBS.put("Mumbai", new double[]{19.08, 72.88}); HUBS.put("Delhi", new double[]{28.61, 77.21});
    }
    private final JdbcTemplate jdbc;

    public ShipmentFlowController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private boolean roleOk(Long id, String role) {
        if (id == null) return true;
        Integer n = jdbc.queryForObject("SELECT count(*) FROM users WHERE id = ? AND role = ? AND active", Integer.class, id, role);
        return n != null && n > 0;
    }
    private static String clean(String s, int max) {
        if (s == null || s.isBlank()) return null;
        String t = s.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }
    private void audit(long actor, String action, String detail) { jdbc.update("INSERT INTO audit_logs(actor_id,action,detail) VALUES (?,?,?)", actor, action, detail); }

    @GetMapping("/shipments/new")
    public String form(Model m) {
        m.addAttribute("hubs", HUBS.keySet());
        m.addAttribute("batches", jdbc.queryForList("SELECT b.id, p.name, b.batch_no, b.quantity_on_hand - b.reserved AS available, b.expiry_date FROM batches b "
            + "JOIN products p ON p.id = b.product_id WHERE b.expiry_date >= current_date AND b.quantity_on_hand - b.reserved > 0 ORDER BY p.name, b.expiry_date"));
        m.addAttribute("drivers", jdbc.queryForList("SELECT id, name FROM users WHERE role = 'DRIVER' AND active ORDER BY name"));
        m.addAttribute("tms", jdbc.queryForList("SELECT id, name FROM users WHERE role = 'TRANSPORT_MANAGER' AND active ORDER BY name"));
        m.addAttribute("hospitals", jdbc.queryForList("SELECT id, name FROM users WHERE role = 'HOSPITAL_STAFF' AND active ORDER BY name"));
        return "shipment_new";
    }

    @PostMapping("/shipments")
    @Transactional
    public String create(@RequestParam long batchId, @RequestParam int quantity, @RequestParam String origin, @RequestParam String destination,
                         @RequestParam(required = false) Long driverId, @RequestParam(required = false) Long transportManagerId, @RequestParam(required = false) Long hospitalId,
                         @RequestParam String expected, @RequestParam(required = false) String instructions,
                         @AuthenticationPrincipal AppPrincipal me, RedirectAttributes ra) {
        double[] o = HUBS.get(origin), d = HUBS.get(destination);
        LocalDateTime exp;
        try { exp = LocalDateTime.parse(expected); } catch (Exception e) { exp = null; }
        String err = null;
        if (o == null || d == null || origin.equals(destination)) err = "Choose two different valid locations.";
        else if (quantity <= 0) err = "Quantity must be greater than zero.";
        else if (exp == null || exp.isBefore(LocalDateTime.now())) err = "Expected delivery must be a valid future date and time.";
        else if (!roleOk(driverId, "DRIVER") || !roleOk(transportManagerId, "TRANSPORT_MANAGER") || !roleOk(hospitalId, "HOSPITAL_STAFF")) err = "Selected driver, transport manager or hospital is invalid.";
        if (err == null) {
            List<Map<String, Object>> b = jdbc.queryForList("SELECT product_id, quantity_on_hand - reserved AS avail, expiry_date FROM batches WHERE id = ? FOR UPDATE", batchId);
            if (b.isEmpty()) err = "Batch not found.";
            else if (((java.sql.Date) b.get(0).get("expiry_date")).toLocalDate().isBefore(LocalDate.now())) err = "This batch has expired.";
            else if (((Number) b.get(0).get("avail")).intValue() < quantity) err = "Only " + b.get(0).get("avail") + " units are available in this batch.";
            else {
                jdbc.update("UPDATE batches SET reserved = reserved + ? WHERE id = ?", quantity, batchId);
                Long id = jdbc.queryForObject("INSERT INTO shipments(tracking_no,product_id,batch_id,quantity,origin,destination,driver_id,transport_manager_id,hospital_id,status,"
                    + "start_lat,start_lon,dest_lat,dest_lon,expected_delivery,instructions) VALUES ('CC-' || nextval('tracking_seq'),?,?,?,?,?,?,?,?,'CREATED',?,?,?,?,?,?) RETURNING id",
                    Long.class, b.get(0).get("product_id"), batchId, quantity, origin, destination, driverId, transportManagerId, hospitalId,
                    o[0], o[1], d[0], d[1], java.sql.Timestamp.valueOf(exp), clean(instructions, 300));
                jdbc.update("INSERT INTO shipment_status_history(shipment_id,from_status,to_status,changed_by,note) VALUES (?,NULL,'CREATED',?,'Shipment created, stock reserved')", id, me.id());
                audit(me.id(), "SHIPMENT_CREATED", "Shipment " + id + " qty " + quantity);
                ra.addFlashAttribute("msg", "Shipment created and stock reserved.");
                return "redirect:/shipments/" + id;
            }
        }
        ra.addFlashAttribute("err", err);
        return "redirect:/shipments/new";
    }

    @PostMapping("/shipments/{id}/deliver")
    @Transactional
    public String deliver(@PathVariable long id, @RequestParam int received, @RequestParam(required = false) String note,
                          @AuthenticationPrincipal AppPrincipal me, RedirectAttributes ra) {
        String scope = switch (me.role()) { case DRIVER -> " AND s.driver_id = ?"; case HOSPITAL_STAFF -> " AND s.hospital_id = ?"; default -> ""; };
        Object[] args = scope.isEmpty() ? new Object[]{id} : new Object[]{id, me.id()};
        List<Map<String, Object>> cur = jdbc.queryForList("SELECT s.status, s.quantity, s.batch_id FROM shipments s WHERE s.id = ?" + scope + " FOR UPDATE OF s", args);
        if (cur.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        String from = (String) cur.get(0).get("status");
        int qty = ((Number) cur.get(0).get("quantity")).intValue();
        if (!from.equals("IN_TRANSIT") && !from.equals("DELAYED")) { ra.addFlashAttribute("err", "This shipment is not in transit (it may already be delivered or cancelled)."); return "redirect:/shipments/" + id; }
        if (received < 0 || received > qty) { ra.addFlashAttribute("err", "Received quantity must be between 0 and " + qty + "."); return "redirect:/shipments/" + id; }
        boolean discrepancy = received != qty;
        jdbc.update("UPDATE shipments SET status = 'DELIVERED', delivered_at = now(), received_qty = ?, delivery_note = ?, discrepancy = ? WHERE id = ?", received, clean(note, 500), discrepancy, id);
        if (cur.get(0).get("batch_id") != null)
            jdbc.update("UPDATE batches SET quantity_on_hand = quantity_on_hand - ?, reserved = reserved - ? WHERE id = ?", qty, qty, cur.get(0).get("batch_id"));
        jdbc.update("INSERT INTO shipment_status_history(shipment_id,from_status,to_status,changed_by,note) VALUES (?,?,'DELIVERED',?,?)", id, from, me.id(), "Received " + received + " of " + qty);
        if (discrepancy)
            jdbc.update("INSERT INTO alerts(shipment_id,type,severity,message) VALUES (?,'DISCREPANCY','WARNING',?) ON CONFLICT DO NOTHING", id, "Received " + received + " of " + qty + " units");
        audit(me.id(), "SHIPMENT_DELIVERED", "Shipment " + id + (discrepancy ? " with discrepancy" : ""));
        ra.addFlashAttribute("msg", discrepancy ? "Delivery recorded with a discrepancy report." : "Delivery confirmed.");
        return "redirect:/shipments/" + id;
    }

    @PostMapping("/shipments/{id}/assign")
    public String assign(@PathVariable long id, @RequestParam(required = false) Long driverId, @RequestParam(required = false) Long transportManagerId,
                         @AuthenticationPrincipal AppPrincipal me, RedirectAttributes ra) {
        if (!roleOk(driverId, "DRIVER") || !roleOk(transportManagerId, "TRANSPORT_MANAGER")) ra.addFlashAttribute("err", "Invalid driver or transport manager.");
        else if (jdbc.update("UPDATE shipments SET driver_id = ?, transport_manager_id = ? WHERE id = ? AND status NOT IN ('DELIVERED','CANCELLED')", driverId, transportManagerId, id) > 0) {
            audit(me.id(), "SHIPMENT_ASSIGNED", "Shipment " + id);
            ra.addFlashAttribute("msg", "Assignment updated.");
        } else ra.addFlashAttribute("err", "Shipment cannot be reassigned.");
        return "redirect:/shipments/" + id;
    }
}
