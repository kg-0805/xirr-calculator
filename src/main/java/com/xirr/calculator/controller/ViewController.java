package com.xirr.calculator.controller;

import com.xirr.calculator.model.AccessRequestForm;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class ViewController {

    @GetMapping("/")
    public String dashboard(Model model, Principal principal) {
        model.addAttribute("currentUser", principal != null ? principal.getName() : "Investor");
        return "index";
    }

    @GetMapping("/login")
    public String login(Model model) {
        if (!model.containsAttribute("accessRequestForm")) {
            model.addAttribute("accessRequestForm", new AccessRequestForm());
        }
        return "login";
    }
}
