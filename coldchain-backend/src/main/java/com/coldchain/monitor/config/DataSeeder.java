package com.coldchain.monitor.config;

import com.coldchain.monitor.entity.*;
import com.coldchain.monitor.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the seven demo accounts on first startup (skips any that already exist). Demo password: Admin@123 */
@Component
public class DataSeeder implements CommandLineRunner {
    private final UserRepository repo;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository repo, PasswordEncoder encoder) { this.repo = repo; this.encoder = encoder; }

    @Override
    public void run(String... args) {
        Object[][] demo = {
            {"admin", "Vikram Nair", Role.ADMINISTRATOR}, {"warehouse", "Rahul Sharma", Role.WAREHOUSE_MANAGER},
            {"transport", "Ananya Rao", Role.TRANSPORT_MANAGER}, {"driver", "Kiran Kumar", Role.DRIVER},
            {"hospital", "Priya Singh", Role.HOSPITAL_STAFF}, {"supplier", "Arjun Mehta", Role.SUPPLIER},
            {"inspector", "Sneha Iyer", Role.QUALITY_INSPECTOR}
        };
        for (Object[] d : demo) {
            String email = d[0] + "@coldchain.local";
            if (repo.findByEmailIgnoreCase(email).isPresent() || repo.findByUsernameIgnoreCase((String) d[0]).isPresent()) continue;
            User u = new User();
            u.username = (String) d[0];
            u.name = (String) d[1];
            u.email = email;
            u.role = (Role) d[2];
            u.status = Status.ACTIVE;
            u.password = encoder.encode("Admin@123");
            repo.save(u);
        }
    }
}
