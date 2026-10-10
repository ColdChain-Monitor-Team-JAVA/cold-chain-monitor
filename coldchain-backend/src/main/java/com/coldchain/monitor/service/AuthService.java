package com.coldchain.monitor.service;

import com.coldchain.monitor.dto.Dtos.*;
import com.coldchain.monitor.entity.*;
import com.coldchain.monitor.exception.ApiException;
import com.coldchain.monitor.repository.UserRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Opaque-token auth kept in memory (restarting the backend signs everyone out). */
@Service
public class AuthService {
    private record Pending(Long userId, Instant expires) {}
    private record Session(Long userId, Instant expires) {}

    private final Map<String, Pending> pending = new ConcurrentHashMap<>();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String mfaCode;
    private final long mfaSeconds;

    public AuthService(UserRepository users, PasswordEncoder encoder,
                       @Value("${coldchain.mfa.code}") String mfaCode, @Value("${coldchain.mfa.expiry-seconds}") long mfaSeconds) {
        this.users = users; this.encoder = encoder; this.mfaCode = mfaCode; this.mfaSeconds = mfaSeconds;
    }

    private String newToken() { byte[] b = new byte[32]; random.nextBytes(b); return Base64.getUrlEncoder().withoutPadding().encodeToString(b); }
    private MfaChallenge challenge(String token) {
        return new MfaChallenge(token, mfaSeconds, "Development/demo MFA: not production email/SMS verification.");
    }

    public MfaChallenge login(LoginRequest r) {
        User u = users.findByEmailIgnoreCase(r.email().trim()).orElse(null);
        if (u == null || !encoder.matches(r.password(), u.password)) throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        if (u.status != Status.ACTIVE) throw new ApiException(HttpStatus.FORBIDDEN, "Your account is currently disabled. Please contact an administrator.");
        if (r.role() != null && r.role() != u.role) throw new ApiException(HttpStatus.FORBIDDEN, "Selected role does not match this account.");
        String token = newToken();
        pending.put(token, new Pending(u.id, Instant.now().plusSeconds(mfaSeconds)));
        return challenge(token);
    }

    public MfaChallenge resend(String mfaToken) {
        Pending p = pending.get(mfaToken);
        if (p == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "Sign-in session not found. Please sign in again.");
        pending.put(mfaToken, new Pending(p.userId(), Instant.now().plusSeconds(mfaSeconds)));
        return challenge(mfaToken);
    }

    public AuthResponse verify(MfaVerifyRequest r) {
        Pending p = pending.get(r.mfaToken());
        if (p == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "Sign-in session not found. Please sign in again.");
        if (Instant.now().isAfter(p.expires())) throw new ApiException(HttpStatus.UNAUTHORIZED, "MFA code has expired. Please request a new code.");
        if (!mfaCode.equals(r.code().trim())) throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid MFA code.");
        pending.remove(r.mfaToken());
        User u = users.findById(p.userId()).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password."));
        if (u.status != Status.ACTIVE) throw new ApiException(HttpStatus.FORBIDDEN, "Your account is currently disabled. Please contact an administrator.");
        u.lastLogin = Instant.now();
        users.save(u);
        String token = newToken();
        sessions.put(token, new Session(u.id, Instant.now().plus(Duration.ofHours(8))));
        return new AuthResponse(token, UserResponse.of(u));
    }

    public Optional<User> resolve(String token) {
        Session s = sessions.get(token);
        if (s == null) return Optional.empty();
        if (Instant.now().isAfter(s.expires())) { sessions.remove(token); return Optional.empty(); }
        return users.findById(s.userId());
    }

    public void logout(String token) { if (token != null) sessions.remove(token); }
}
