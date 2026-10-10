package com.coldchain.monitor.controller;

import com.coldchain.monitor.dto.Dtos.*;
import com.coldchain.monitor.entity.Status;
import com.coldchain.monitor.entity.User;
import com.coldchain.monitor.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Administrator only (enforced in SecurityConfig). */
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService users;

    public UserController(UserService users) { this.users = users; }

    @GetMapping public List<UserResponse> list() { return users.list(); }
    @GetMapping("/{id}") public UserResponse get(@PathVariable Long id) { return users.get(id); }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest r) { return ResponseEntity.status(HttpStatus.CREATED).body(users.create(r)); }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest r, @AuthenticationPrincipal User me) { return users.update(id, r, me); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal User me) { users.delete(id, me); return ResponseEntity.noContent().build(); }

    @PatchMapping("/{id}/activate")
    public UserResponse activate(@PathVariable Long id, @AuthenticationPrincipal User me) { return users.setStatus(id, Status.ACTIVE, me); }

    @PatchMapping("/{id}/deactivate")
    public UserResponse deactivate(@PathVariable Long id, @AuthenticationPrincipal User me) { return users.setStatus(id, Status.INACTIVE, me); }
}
