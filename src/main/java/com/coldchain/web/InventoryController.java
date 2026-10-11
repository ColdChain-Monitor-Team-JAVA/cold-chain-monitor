package com.coldchain.web;

import java.time.LocalDate;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Products and batch-level inventory. Admin/Warehouse manage everything; Suppliers manage their own products; Quality Inspectors read. */
@Controller
@RequestMapping("/inventory")
public class InventoryController {
    private static final int PAGE = 15;
    private final JdbcTemplate jdbc;

    public InventoryController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static boolean manager(AppPrincipal me) { return me.role() == Role.ADMIN || me.role() == Role.WAREHOUSE_MANAGER; }
    private boolean owns(AppPrincipal me, long productId) {
        if (manager(me)) return true;
        Integer n = jdbc.queryForObject("SELECT count(*) FROM products WHERE id = ? AND supplier_id = ?", Integer.class, productId, me.id());
        return n != null && n > 0;
    }
    private void audit(long actor, String action, String detail) { jdbc.update("INSERT INTO audit_logs(actor_id,action,detail) VALUES (?,?,?)", actor, action, detail); }
    private static String blank(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    @GetMapping
    public String list(@RequestParam(required = false) String q, @RequestParam(required = false) String filter,
                       @RequestParam(defaultValue = "0") int page, Model m, @AuthenticationPrincipal AppPrincipal me) {
        StringBuilder w = new StringBuilder(" WHERE 1=1");
        List<Object> a = new ArrayList<>();
        if (me.role() == Role.SUPPLIER) { w.append(" AND p.supplier_id = ?"); a.add(me.id()); }
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase() + "%";
            w.append(" AND (lower(p.code) LIKE ? OR lower(p.name) LIKE ? OR lower(b.batch_no) LIKE ?)");
            a.add(like); a.add(like); a.add(like);
        }
        if ("expired".equals(filter)) w.append(" AND b.expiry_date < current_date");
        else if ("low".equals(filter)) w.append(" AND b.quantity_on_hand - b.reserved <= p.reorder_level");
        String from = " FROM batches b JOIN products p ON p.id = b.product_id" + w;
        Integer total = jdbc.queryForObject("SELECT count(*)" + from, Integer.class, a.toArray());
        int pg = Math.max(0, page);
        List<Object> a2 = new ArrayList<>(a); a2.add(PAGE); a2.add(pg * PAGE);
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT b.id, p.code, p.name, p.unit, p.reorder_level, b.batch_no, b.quantity_on_hand, b.reserved, b.expiry_date, b.storage_location"
            + from + " ORDER BY b.expiry_date, b.id LIMIT ? OFFSET ?", a2.toArray());
        for (Map<String, Object> r : rows) {
            int avail = ((Number) r.get("quantity_on_hand")).intValue() - ((Number) r.get("reserved")).intValue();
            r.put("available", avail);
            r.put("stock", Rules.stockStatus(avail, ((Number) r.get("reorder_level")).intValue(), ((java.sql.Date) r.get("expiry_date")).toLocalDate(), LocalDate.now()));
        }
        m.addAttribute("rows", rows); m.addAttribute("page", pg); m.addAttribute("pages", Math.max(1, ((total == null ? 0 : total) + PAGE - 1) / PAGE));
        m.addAttribute("q", q); m.addAttribute("filter", filter);
        boolean sup = me.role() == Role.SUPPLIER;
        m.addAttribute("products", jdbc.queryForList("SELECT p.id, p.code, p.name, p.category, p.description, p.unit, p.reorder_level, p.min_temp, p.max_temp, p.min_humidity, p.max_humidity, u.name AS supplier "
            + "FROM products p LEFT JOIN users u ON u.id = p.supplier_id" + (sup ? " WHERE p.supplier_id = ?" : "") + " ORDER BY p.code", sup ? new Object[]{me.id()} : new Object[0]));
        m.addAttribute("suppliers", jdbc.queryForList("SELECT id, name FROM users WHERE role = 'SUPPLIER' AND active ORDER BY name"));
        m.addAttribute("canWrite", manager(me) || sup); m.addAttribute("canDelete", manager(me)); m.addAttribute("isSupplier", sup);
        return "inventory";
    }

    private static String validate(String code, String name, String category, String unit, int reorder, double minT, double maxT, double minH, double maxH) {
        if (code != null && (code.isBlank() || code.length() > 30)) return "Product code is required (max 30 characters).";
        if (name.isBlank() || name.length() > 120) return "Product name is required (max 120 characters).";
        if (category.isBlank() || category.length() > 60) return "Category is required (max 60 characters).";
        if (unit.isBlank() || unit.length() > 20) return "Unit is required (max 20 characters).";
        if (reorder < 0) return "Reorder level cannot be negative.";
        if (minT >= maxT) return "Minimum temperature must be lower than maximum temperature.";
        if (minH >= maxH || minH < 0 || maxH > 100) return "Humidity range must be within 0-100 with minimum below maximum.";
        return null;
    }

    @PostMapping("/products")
    public String createProduct(@RequestParam String code, @RequestParam String name, @RequestParam String category, @RequestParam(required = false) String description,
                                @RequestParam String unit, @RequestParam int reorderLevel, @RequestParam double minTemp, @RequestParam double maxTemp,
                                @RequestParam double minHumidity, @RequestParam double maxHumidity, @RequestParam(required = false) Long supplierId,
                                @AuthenticationPrincipal AppPrincipal me, RedirectAttributes ra) {
        String err = validate(code.trim(), name.trim(), category.trim(), unit.trim(), reorderLevel, minTemp, maxTemp, minHumidity, maxHumidity);
        if (err != null) { ra.addFlashAttribute("err", err); return "redirect:/inventory"; }
        Long sup = me.role() == Role.SUPPLIER ? Long.valueOf(me.id()) : supplierId;
        try {
            jdbc.update("INSERT INTO products(code,name,category,description,unit,reorder_level,min_temp,max_temp,min_humidity,max_humidity,supplier_id) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                code.trim().toUpperCase(), name.trim(), category.trim(), blank(description), unit.trim(), reorderLevel, minTemp, maxTemp, minHumidity, maxHumidity, sup);
            audit(me.id(), "PRODUCT_CREATED", code.trim().toUpperCase());
            ra.addFlashAttribute("msg", "Product created.");
        } catch (DuplicateKeyException e) { ra.addFlashAttribute("err", "A product with this code already exists."); }
        return "redirect:/inventory";
    }

    @PostMapping("/products/{id}/update")
    public String updateProduct(@PathVariable long id, @RequestParam String name, @RequestParam String category, @RequestParam(required = false) String description,
                                @RequestParam String unit, @RequestParam int reorderLevel, @RequestParam double minTemp, @RequestParam double maxTemp,
                                @RequestParam double minHumidity, @RequestParam double maxHumidity, @AuthenticationPrincipal AppPrincipal me, RedirectAttributes ra) {
        if (!owns(me, id)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        String err = validate(null, name.trim(), category.trim(), unit.trim(), reorderLevel, minTemp, maxTemp, minHumidity, maxHumidity);
        if (err != null) { ra.addFlashAttribute("err", err); return "redirect:/inventory"; }
        jdbc.update("UPDATE products SET name=?, category=?, description=?, unit=?, reorder_level=?, min_temp=?, max_temp=?, min_humidity=?, max_humidity=? WHERE id=?",
            name.trim(), category.trim(), blank(description), unit.trim(), reorderLevel, minTemp, maxTemp, minHumidity, maxHumidity, id);
        audit(me.id(), "PRODUCT_UPDATED", "Product " + id);
        ra.addFlashAttribute("msg", "Product updated.");
        return "redirect:/inventory";
    }

    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable long id, @AuthenticationPrincipal AppPrincipal me, RedirectAttributes ra) {
        if (!manager(me)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        Integer refs = jdbc.queryForObject("SELECT (SELECT count(*) FROM batches WHERE product_id = ?) + (SELECT count(*) FROM shipments WHERE product_id = ?)", Integer.class, id, id);
        if (refs != null && refs > 0) ra.addFlashAttribute("err", "Cannot delete: this product has batches or shipments.");
        else if (jdbc.update("DELETE FROM products WHERE id = ?", id) > 0) { audit(me.id(), "PRODUCT_DELETED", "Product " + id); ra.addFlashAttribute("msg", "Product deleted."); }
        return "redirect:/inventory";
    }

    @PostMapping("/batches")
    public String createBatch(@RequestParam long productId, @RequestParam String batchNo, @RequestParam int quantity, @RequestParam LocalDate expiry,
                              @RequestParam String location, @AuthenticationPrincipal AppPrincipal me, RedirectAttributes ra) {
        if (!owns(me, productId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (batchNo.isBlank() || batchNo.length() > 40) ra.addFlashAttribute("err", "Batch number is required (max 40 characters).");
        else if (quantity < 0) ra.addFlashAttribute("err", "Quantity cannot be negative.");
        else if (location.isBlank() || location.length() > 80) ra.addFlashAttribute("err", "Storage location is required (max 80 characters).");
        else {
            try {
                jdbc.update("INSERT INTO batches(product_id,batch_no,quantity_on_hand,expiry_date,storage_location) VALUES (?,?,?,?,?)", productId, batchNo.trim(), quantity, java.sql.Date.valueOf(expiry), location.trim());
                audit(me.id(), "BATCH_CREATED", batchNo.trim());
                ra.addFlashAttribute("msg", "Batch added.");
            } catch (DuplicateKeyException e) { ra.addFlashAttribute("err", "This product already has a batch with that number."); }
        }
        return "redirect:/inventory";
    }

    @PostMapping("/batches/{id}/update")
    @Transactional
    public String updateBatch(@PathVariable long id, @RequestParam int quantity, @RequestParam LocalDate expiry, @RequestParam String location,
                              @AuthenticationPrincipal AppPrincipal me, RedirectAttributes ra) {
        List<Map<String, Object>> r = jdbc.queryForList("SELECT product_id, reserved FROM batches WHERE id = ? FOR UPDATE", id);
        if (r.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (!owns(me, ((Number) r.get(0).get("product_id")).longValue())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        int reserved = ((Number) r.get(0).get("reserved")).intValue();
        if (quantity < reserved) ra.addFlashAttribute("err", "Quantity cannot be below the reserved amount (" + reserved + ").");
        else if (location.isBlank() || location.length() > 80) ra.addFlashAttribute("err", "Storage location is required (max 80 characters).");
        else {
            jdbc.update("UPDATE batches SET quantity_on_hand = ?, expiry_date = ?, storage_location = ? WHERE id = ?", quantity, java.sql.Date.valueOf(expiry), location.trim(), id);
            audit(me.id(), "BATCH_UPDATED", "Batch " + id);
            ra.addFlashAttribute("msg", "Batch updated.");
        }
        return "redirect:/inventory";
    }

    @PostMapping("/batches/{id}/delete")
    public String deleteBatch(@PathVariable long id, @AuthenticationPrincipal AppPrincipal me, RedirectAttributes ra) {
        if (!manager(me)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        Integer refs = jdbc.queryForObject("SELECT (SELECT count(*) FROM shipments WHERE batch_id = ?) + (SELECT count(*) FROM alerts WHERE batch_id = ?) + (SELECT reserved FROM batches WHERE id = ?)", Integer.class, id, id, id);
        if (refs != null && refs > 0) ra.addFlashAttribute("err", "Cannot delete: the batch is referenced by shipments or alerts, or has reserved stock.");
        else if (jdbc.update("DELETE FROM batches WHERE id = ?", id) > 0) { audit(me.id(), "BATCH_DELETED", "Batch " + id); ra.addFlashAttribute("msg", "Batch deleted."); }
        return "redirect:/inventory";
    }
}
