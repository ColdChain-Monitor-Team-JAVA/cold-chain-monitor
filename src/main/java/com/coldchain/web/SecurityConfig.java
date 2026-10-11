package com.coldchain.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    SecurityFilterChain chain(HttpSecurity http, MfaService mfa) throws Exception {
        http.authorizeHttpRequests(a -> a
                .requestMatchers("/login", "/css/**", "/error").permitAll()
                .requestMatchers("/users/**", "/dashboard/admin").hasRole("ADMIN")
                .requestMatchers("/dashboard/warehouse").hasRole("WAREHOUSE_MANAGER")
                .requestMatchers("/dashboard/transport").hasRole("TRANSPORT_MANAGER")
                .requestMatchers("/dashboard/driver").hasRole("DRIVER")
                .requestMatchers("/dashboard/hospital").hasRole("HOSPITAL_STAFF")
                .requestMatchers("/dashboard/supplier").hasRole("SUPPLIER")
                .requestMatchers("/dashboard/inspector").hasRole("QUALITY_INSPECTOR")
                .requestMatchers(HttpMethod.GET, "/inventory").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "SUPPLIER", "QUALITY_INSPECTOR")
                .requestMatchers(HttpMethod.POST, "/inventory/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "SUPPLIER")
                .requestMatchers("/shipments/new").hasAnyRole("ADMIN", "TRANSPORT_MANAGER", "WAREHOUSE_MANAGER")
                .requestMatchers(HttpMethod.POST, "/shipments").hasAnyRole("ADMIN", "TRANSPORT_MANAGER", "WAREHOUSE_MANAGER")
                .requestMatchers(HttpMethod.POST, "/shipments/*/deliver").hasAnyRole("ADMIN", "TRANSPORT_MANAGER", "DRIVER", "HOSPITAL_STAFF")
                .requestMatchers(HttpMethod.POST, "/shipments/*/assign").hasAnyRole("ADMIN", "TRANSPORT_MANAGER")
                .requestMatchers(HttpMethod.POST, "/alerts/*/note").hasAnyRole("ADMIN", "QUALITY_INSPECTOR")
                .requestMatchers("/reports/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "TRANSPORT_MANAGER", "QUALITY_INSPECTOR")
                .requestMatchers(HttpMethod.POST, "/alerts/*/acknowledge").hasAnyRole("ADMIN", "QUALITY_INSPECTOR", "TRANSPORT_MANAGER", "WAREHOUSE_MANAGER")
                .requestMatchers(HttpMethod.POST, "/alerts/*/resolve").hasAnyRole("ADMIN", "QUALITY_INSPECTOR")
                .requestMatchers(HttpMethod.POST, "/shipments/*/status").hasAnyRole("ADMIN", "TRANSPORT_MANAGER", "DRIVER")
                .anyRequest().authenticated())
            .formLogin(f -> f.loginPage("/login")
                .successHandler((req, res, auth) -> {
                    // The dropdown is only a convenience check; the real role always comes from the database record.
                    String selected = req.getParameter("role");
                    AppPrincipal p = (AppPrincipal) auth.getPrincipal();
                    if (selected == null || !selected.equals(p.role().name())) {
                        SecurityContextHolder.clearContext();
                        HttpSession old = req.getSession(false);
                        if (old != null) old.invalidate();
                        res.sendRedirect(req.getContextPath() + "/login?roleMismatch");
                        return;
                    }
                    mfa.start(req.getSession());
                    res.sendRedirect(req.getContextPath() + "/mfa");
                })
                .failureHandler((req, res, ex) -> res.sendRedirect(req.getContextPath() + (ex instanceof DisabledException ? "/login?disabled" : "/login?error"))))
            .logout(l -> l.logoutSuccessUrl("/login?logout").invalidateHttpSession(true).deleteCookies("JSESSIONID"))
            .addFilterBefore(new MfaGateFilter(), AuthorizationFilter.class);
        return http.build();
    }
}
