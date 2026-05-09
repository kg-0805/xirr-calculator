package com.xirr.calculator.controller;

import com.xirr.calculator.model.AccessRequestForm;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class ViewController {

    @GetMapping("/")
    public String dashboard(Model model, Principal principal, Authentication authentication) {
        model.addAttribute("currentUser", principal != null ? principal.getName() : "Investor");
        boolean isAdmin = authentication != null &&
                authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        model.addAttribute("isAdmin", isAdmin);
        return "index";
    }

    @GetMapping("/login")
    public String login(Model model) {
        if (!model.containsAttribute("accessRequestForm")) {
            model.addAttribute("accessRequestForm", new AccessRequestForm());
        }
        return "login";
    }

    @GetMapping("/disclaimer")
    public String disclaimer() {
        return "disclaimer";
    }

    @GetMapping("/privacy")
    public String privacy() {
        return "privacy";
    }
}
