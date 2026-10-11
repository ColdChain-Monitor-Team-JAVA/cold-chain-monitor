package com.coldchain.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** A password-authenticated user cannot reach any page until the MFA step is completed. */
public class MfaGateFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        boolean authed = a != null && a.isAuthenticated() && !(a instanceof AnonymousAuthenticationToken);
        HttpSession s = req.getSession(false);
        boolean verified = s != null && Boolean.TRUE.equals(s.getAttribute(MfaService.OK_ATTR));
        String p = req.getRequestURI();
        boolean open = p.equals("/mfa") || p.equals("/logout") || p.equals("/error") || p.startsWith("/css/");
        if (authed && !verified && !open) { res.sendRedirect(req.getContextPath() + "/mfa"); return; }
        chain.doFilter(req, res);
    }
}
