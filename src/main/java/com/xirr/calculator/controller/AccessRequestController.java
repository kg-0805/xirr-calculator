package com.xirr.calculator.controller;

import com.xirr.calculator.exception.AccessRequestStorageException;
import com.xirr.calculator.model.AccessRequestForm;
import com.xirr.calculator.service.AccessRequestService;
import com.xirr.calculator.service.EmailNotificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/access-request")
public class AccessRequestController {

    private final AccessRequestService accessRequestService;
    private final EmailNotificationService emailNotificationService;

    public AccessRequestController(AccessRequestService accessRequestService,
                                   EmailNotificationService emailNotificationService) {
        this.accessRequestService = accessRequestService;
        this.emailNotificationService = emailNotificationService;
    }

    @PostMapping
    public String submit(@Valid AccessRequestForm accessRequestForm,
                         BindingResult bindingResult,
                         HttpServletRequest request,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("requestFailureMessage", "Please fix the highlighted fields and submit again.");
            return "login";
        }

        try {
            accessRequestService.submitRequest(accessRequestForm, resolveClientIp(request));
            emailNotificationService.sendAccessRequestNotification(accessRequestForm, resolveClientIp(request));
        } catch (AccessRequestStorageException exception) {
            model.addAttribute("requestFailureMessage", "We could not record your request right now. Please try again later.");
            return "login";
        }

        redirectAttributes.addFlashAttribute("requestSubmittedEmail", accessRequestForm.getEmail());
        return "redirect:/login?requestSuccess";
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }
}
