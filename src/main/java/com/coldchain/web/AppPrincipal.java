package com.coldchain.web;

import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;

public class AppPrincipal extends User {
    private final long id;
    private final String displayName;
    private final Role role;

    public AppPrincipal(long id, String name, String email, String hash, Role role, boolean active) {
        super(email, hash, active, true, true, true, AuthorityUtils.createAuthorityList("ROLE_" + role.name()));
        this.id = id; this.displayName = name; this.role = role;
    }
    public long id() { return id; }
    public String displayName() { return displayName; }
    public Role role() { return role; }
}
