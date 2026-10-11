package com.coldchain.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Idempotent seed of seven local-development demo users plus sample products and shipments. */
@Component
public class DemoDataSeeder implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder enc;
    private final String demoPassword;

    public DemoDataSeeder(JdbcTemplate jdbc, PasswordEncoder enc, @Value("${coldchain.demo-password}") String demoPassword) {
        this.jdbc = jdbc; this.enc = enc; this.demoPassword = demoPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        String[][] users = {{"admin", "Vikram Nair", "ADMIN"}, {"warehouse", "Rahul Sharma", "WAREHOUSE_MANAGER"},
            {"transport", "Ananya Rao", "TRANSPORT_MANAGER"}, {"driver", "Kiran Kumar", "DRIVER"},
            {"hospital", "Priya Singh", "HOSPITAL_STAFF"}, {"supplier", "Arjun Mehta", "SUPPLIER"},
            {"inspector", "Sneha Iyer", "QUALITY_INSPECTOR"}};
        for (String[] u : users) {
            String email = u[0] + "@coldchain.local";
            Integer n = jdbc.queryForObject("SELECT count(*) FROM users WHERE lower(email) = ?", Integer.class, email);
            if (n != null && n == 0) jdbc.update("INSERT INTO users(name,email,password_hash,role) VALUES (?,?,?,?)", u[1], email, enc.encode(demoPassword), u[2]);
        }
        String sup = "(SELECT id FROM users WHERE email = 'supplier@coldchain.local')";
        jdbc.update("INSERT INTO products(code,name,min_temp,max_temp,min_humidity,max_humidity,supplier_id) VALUES ('VAC-FLU','Influenza Vaccine',2,8,30,70," + sup + ") ON CONFLICT (code) DO NOTHING");
        jdbc.update("INSERT INTO products(code,name,min_temp,max_temp,min_humidity,max_humidity,supplier_id) VALUES ('INS-100','Insulin Vials',2,8,30,70," + sup + ") ON CONFLICT (code) DO NOTHING");
        jdbc.update("INSERT INTO products(code,name,min_temp,max_temp,min_humidity,max_humidity,supplier_id) VALUES ('PLS-FRZ','Frozen Plasma',-30,-18,20,60," + sup + ") ON CONFLICT (code) DO NOTHING");
        Object[][] ships = {
            {"CC-1001", "VAC-FLU", 200, "Bengaluru", "Chennai", "IN_TRANSIT", 12.97, 77.59, 13.08, 80.27},
            {"CC-1002", "INS-100", 150, "Mysuru", "Hyderabad", "IN_TRANSIT", 12.30, 76.64, 17.38, 78.48},
            {"CC-1003", "PLS-FRZ", 60, "Bengaluru", "Mysuru", "CREATED", 12.97, 77.59, 12.30, 76.64}};
        for (Object[] s : ships) {
            jdbc.update("INSERT INTO shipments(tracking_no,product_id,quantity,origin,destination,driver_id,status,start_lat,start_lon,dest_lat,dest_lon) "
                + "VALUES (?, (SELECT id FROM products WHERE code = ?), ?, ?, ?, (SELECT id FROM users WHERE email = 'driver@coldchain.local'), ?, ?, ?, ?, ?) "
                + "ON CONFLICT (tracking_no) DO NOTHING", s);
        }
        jdbc.update("UPDATE shipments SET hospital_id = (SELECT id FROM users WHERE email = 'hospital@coldchain.local'), "
            + "transport_manager_id = (SELECT id FROM users WHERE email = 'transport@coldchain.local'), expected_delivery = now() + interval '2 days' "
            + "WHERE tracking_no IN ('CC-1001','CC-1002','CC-1003') AND hospital_id IS NULL");
        Object[][] batches = {{"VAC-FLU", "FLU-2601", 400, 120, "Cold Room A"}, {"VAC-FLU", "FLU-2509", 30, -5, "Cold Room A"},
            {"INS-100", "INS-2603", 250, 200, "Cold Room B"}, {"PLS-FRZ", "PLS-2602", 15, 90, "Freezer 1"}};
        for (Object[] b : batches) {
            jdbc.update("INSERT INTO batches(product_id,batch_no,quantity_on_hand,expiry_date,storage_location) "
                + "VALUES ((SELECT id FROM products WHERE code = ?), ?, ?, current_date + ?, ?) ON CONFLICT (product_id, batch_no) DO NOTHING", b);
        }
    }
}
