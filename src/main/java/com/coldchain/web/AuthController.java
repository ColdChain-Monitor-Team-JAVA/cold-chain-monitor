package com.coldchain.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {
    private final MfaService mfa;
    public AuthController(MfaService mfa) { this.mfa = mfa; }

    @GetMapping("/login") public String login(Model m) { m.addAttribute("roles", Role.values()); return "login"; }
    @GetMapping("/mfa") public String mfaPage() { return "mfa"; }

    @PostMapping("/mfa")
    public String verify(@RequestParam String code, HttpServletRequest req, Model m) {
        HttpSession s = req.getSession();
        switch (mfa.verify(s, code.trim())) {
            case OK: return "redirect:/dashboard";
            case WRONG: m.addAttribute("error", "Invalid MFA code."); return "mfa";
            default: s.invalidate(); SecurityContextHolder.clearContext(); return "redirect:/login?mfaFailed";
        }
    }
}
