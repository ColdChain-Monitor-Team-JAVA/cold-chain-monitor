package com.coldchain.web;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** The role always comes from the database record, never from the browser. */
@Service
public class AppUserDetailsService implements UserDetailsService {
    private final JdbcTemplate jdbc;
    public AppUserDetailsService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public UserDetails loadUserByUsername(String email) {
        List<AppPrincipal> rows = jdbc.query(
            "SELECT id, name, email, password_hash, role, active FROM users WHERE lower(email) = lower(?)",
            (rs, i) -> new AppPrincipal(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), Role.valueOf(rs.getString(5)), rs.getBoolean(6)),
            email.trim());
        if (rows.isEmpty()) throw new UsernameNotFoundException("Unknown user");
        return rows.get(0);
    }
}
