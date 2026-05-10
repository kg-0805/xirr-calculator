package com.xirr.calculator.controller;

import com.xirr.calculator.model.AccessRequestForm;
import com.xirr.calculator.model.AppUser;
import com.xirr.calculator.repository.UserRepository;
import com.xirr.calculator.service.AccessRequestService;
import com.xirr.calculator.service.EmailNotificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Controller
@RequestMapping("/access-request")
public class AccessRequestController {

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$%";
    private static final int PASSWORD_LENGTH = 12;

    private final AccessRequestService accessRequestService;
    private final EmailNotificationService emailNotificationService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public AccessRequestController(AccessRequestService accessRequestService,
                                   EmailNotificationService emailNotificationService,
                                   UserRepository userRepository,
                                   PasswordEncoder passwordEncoder) {
        this.accessRequestService = accessRequestService;
        this.emailNotificationService = emailNotificationService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping
    public String submit(@Valid AccessRequestForm accessRequestForm,
                         BindingResult bindingResult,
                         HttpServletRequest request,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        String email = accessRequestForm.getEmail() != null ? accessRequestForm.getEmail().toLowerCase().trim() : "";

        if (userRepository.existsByEmail(email)) {
            bindingResult.rejectValue("email", "duplicate", "An account with this email already exists.");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("requestFailureMessage", "Please fix the highlighted fields and submit again.");
            return "login";
        }

        // Store the request
        try {
            accessRequestService.submitRequest(accessRequestForm, resolveClientIp(request));
        } catch (Exception ignored) {}

        // Auto-create user with random password
        String password = generatePassword();
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);
        AppUser user = new AppUser(email, accessRequestForm.getFullName().trim(), passwordEncoder.encode(password), true, false, expiresAt);
        userRepository.save(user);

        // Send welcome email and admin notification
        emailNotificationService.sendUserCreatedNotification(user.getFullName(), email, password, expiresAt);
        emailNotificationService.sendAccessRequestNotification(accessRequestForm, resolveClientIp(request));

        redirectAttributes.addFlashAttribute("requestSubmittedEmail", email);
        return "redirect:/login?requestSuccess";
    }

    private String generatePassword() {
        StringBuilder sb = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            sb.append(PASSWORD_CHARS.charAt(secureRandom.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
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
