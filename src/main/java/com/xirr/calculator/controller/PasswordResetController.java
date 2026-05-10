package com.xirr.calculator.controller;

import com.xirr.calculator.model.AppUser;
import com.xirr.calculator.repository.UserRepository;
import com.xirr.calculator.service.EmailNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@RestController
@RequestMapping("/api/password-reset")
public class PasswordResetController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailNotificationService emailNotificationService;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                   EmailNotificationService emailNotificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailNotificationService = emailNotificationService;
    }

    @PostMapping("/request-code")
    public ResponseEntity<?> requestCode(@RequestBody Map<String, String> payload) {
        String email = payload.getOrDefault("email", "").toLowerCase().trim();
        AppUser user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "No account found with that email."));
        }

        if (!user.isActive()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Your account is inactive. Please request reactivation."));
        }

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        user.setResetCode(code);
        user.setResetCodeExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        userRepository.save(user);

        emailNotificationService.sendResetCodeEmail(email, code);
        return ResponseEntity.ok(Map.of("message", "Verification code sent to your email."));
    }

    @PostMapping("/verify-and-reset")
    public ResponseEntity<?> verifyAndReset(@RequestBody Map<String, String> payload) {
        String email = payload.getOrDefault("email", "").toLowerCase().trim();
        String code = payload.getOrDefault("code", "").trim();
        String newPassword = payload.getOrDefault("newPassword", "");

        if (email.isBlank() || code.isBlank() || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "All fields are required."));
        }

        AppUser user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid request."));
        }

        if (user.getResetCode() == null || !user.getResetCode().equals(code)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid verification code."));
        }

        if (user.getResetCodeExpiresAt() == null || Instant.now().isAfter(user.getResetCodeExpiresAt())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Verification code has expired."));
        }

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            return ResponseEntity.badRequest().body(Map.of("message", "New password cannot be the same as the current password."));
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setResetCode(null);
        user.setResetCodeExpiresAt(null);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password reset successfully. You can now log in."));
    }

    @PostMapping("/request-admin-reset")
    public ResponseEntity<?> requestAdminReset(@RequestBody Map<String, String> payload) {
        String email = payload.getOrDefault("email", "").toLowerCase().trim();
        AppUser user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "No account found with that email."));
        }

        emailNotificationService.sendAdminPasswordResetRequest(user.getFullName(), user.getEmail());
        return ResponseEntity.ok(Map.of("message", "Your request has been sent to the admin. You will be contacted shortly."));
    }

    @PostMapping("/request-reactivation")
    public ResponseEntity<?> requestReactivation(@RequestBody Map<String, String> payload) {
        String email = payload.getOrDefault("email", "").toLowerCase().trim();
        String reason = payload.getOrDefault("reason", "").trim();

        if (email.isBlank() || reason.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email and reason are required."));
        }

        AppUser user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "No account found with that email."));
        }

        if (user.isActive() && !user.isExpired()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Your account is already active."));
        }

        emailNotificationService.sendReactivationRequest(user.getFullName(), user.getEmail(), reason);
        return ResponseEntity.ok(Map.of("message", "Reactivation request sent to admin. You will be notified once approved."));
    }
}
