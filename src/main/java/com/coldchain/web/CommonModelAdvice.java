package com.coldchain.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

@ControllerAdvice
public class CommonModelAdvice {
    @ModelAttribute
    public void common(Model m, @AuthenticationPrincipal AppPrincipal me) {
        if (me != null) { m.addAttribute("meName", me.displayName()); m.addAttribute("meRole", me.role().label()); }
    }
}
