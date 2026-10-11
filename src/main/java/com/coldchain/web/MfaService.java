package com.coldchain.web;

import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Demonstration MFA: a configurable fixed code with expiry and attempt limit (not email/SMS). */
@Component
public class MfaService {
    public enum Result { OK, WRONG, EXPIRED, LOCKED }
    public static final String OK_ATTR = "MFA_OK";
    private final String code;
    private final long expirySeconds;
    private final int maxAttempts;

    public MfaService(@Value("${coldchain.mfa.code}") String code, @Value("${coldchain.mfa.expiry-seconds}") long expirySeconds,
                      @Value("${coldchain.mfa.max-attempts}") int maxAttempts) {
        this.code = code; this.expirySeconds = expirySeconds; this.maxAttempts = maxAttempts;
    }

    public void start(HttpSession s) {
        s.removeAttribute(OK_ATTR);
        s.setAttribute("MFA_EXPIRES", Instant.now().plusSeconds(expirySeconds));
        s.setAttribute("MFA_ATTEMPTS", 0);
    }

    public Result verify(HttpSession s, String input) {
        Instant exp = (Instant) s.getAttribute("MFA_EXPIRES");
        Integer tries = (Integer) s.getAttribute("MFA_ATTEMPTS");
        if (exp == null || tries == null || Instant.now().isAfter(exp)) return Result.EXPIRED;
        if (tries >= maxAttempts) return Result.LOCKED;
        if (MessageDigest.isEqual(code.getBytes(StandardCharsets.UTF_8), input.getBytes(StandardCharsets.UTF_8))) {
            s.setAttribute(OK_ATTR, Boolean.TRUE);
            return Result.OK;
        }
        s.setAttribute("MFA_ATTEMPTS", tries + 1);
        return tries + 1 >= maxAttempts ? Result.LOCKED : Result.WRONG;
    }
}
