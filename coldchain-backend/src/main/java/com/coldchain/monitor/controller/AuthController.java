package com.coldchain.monitor.controller;

import com.coldchain.monitor.dto.Dtos.*;
import com.coldchain.monitor.entity.User;
import com.coldchain.monitor.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/login")
    public MfaChallenge login(@Valid @RequestBody LoginRequest r) { return auth.login(r); }

    @PostMapping("/mfa/verify")
    public AuthResponse verify(@Valid @RequestBody MfaVerifyRequest r) { return auth.verify(r); }

    @PostMapping("/mfa/resend")
    public MfaChallenge resend(@Valid @RequestBody MfaResendRequest r) { return auth.resend(r.mfaToken()); }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal User me) { return UserResponse.of(me); }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String header) {
        if (header != null && header.startsWith("Bearer ")) auth.logout(header.substring(7));
        return ResponseEntity.noContent().build();
    }
}
