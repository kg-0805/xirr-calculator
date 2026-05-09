package com.xirr.calculator.controller;

import com.xirr.calculator.model.AppUser;
import com.xirr.calculator.repository.UserRepository;
import com.xirr.calculator.service.EmailNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Controller
public class AdminController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailNotificationService emailNotificationService;

    public AdminController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           EmailNotificationService emailNotificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailNotificationService = emailNotificationService;
    }

    @GetMapping("/admin")
    public String adminPanel(Model model, Principal principal) {
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("currentUser", principal.getName());
        return "admin";
    }

    @PostMapping("/api/admin/users")
    @ResponseBody
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> payload) {
        String email = payload.getOrDefault("email", "").toLowerCase().trim();
        String fullName = payload.getOrDefault("fullName", "").trim();
        String password = payload.getOrDefault("password", "");
        boolean admin = Boolean.parseBoolean(payload.getOrDefault("admin", "false"));

        if (email.isBlank() || fullName.isBlank() || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "All fields are required."));
        }
        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email already exists."));
        }

        Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);
        AppUser user = new AppUser(email, fullName, passwordEncoder.encode(password), true, admin, expiresAt);
        userRepository.save(user);

        emailNotificationService.sendUserCreatedNotification(fullName, email, password);
        return ResponseEntity.ok(Map.of("message", "User created successfully."));
    }

    @PostMapping("/api/admin/users/{id}/activate")
    @ResponseBody
    public ResponseEntity<?> activateUser(@PathVariable Long id) {
        AppUser user = userRepository.findById(id).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        user.setActive(true);
        userRepository.save(user);

        emailNotificationService.sendUserActivatedNotification(user.getFullName(), user.getEmail());
        return ResponseEntity.ok(Map.of("message", "User activated."));
    }

    @PostMapping("/api/admin/users/{id}/deactivate")
    @ResponseBody
    public ResponseEntity<?> deactivateUser(@PathVariable Long id, Principal principal) {
        AppUser user = userRepository.findById(id).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();
        if (user.getEmail().equals(principal.getName())) {
            return ResponseEntity.badRequest().body(Map.of("message", "You cannot deactivate yourself."));
        }

        user.setActive(false);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "User deactivated."));
    }

    @PostMapping("/api/admin/users/{id}/expiry")
    @ResponseBody
    public ResponseEntity<?> setExpiry(@PathVariable Long id, @RequestBody Map<String, String> payload, Principal principal) {
        AppUser user = userRepository.findById(id).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();
        if (user.getEmail().equals(principal.getName())) {
            return ResponseEntity.badRequest().body(Map.of("message", "You cannot change your own expiry."));
        }

        String expiresAt = payload.getOrDefault("expiresAt", "");
        String hours = payload.getOrDefault("hours", "");

        if (!expiresAt.isBlank()) {
            user.setExpiresAt(Instant.parse(expiresAt));
        } else if (!hours.isBlank()) {
            user.setExpiresAt(Instant.now().plus(Long.parseLong(hours), ChronoUnit.HOURS));
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "Provide expiresAt or hours."));
        }

        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Expiry updated successfully."));
    }

    @PostMapping("/api/admin/users/{id}/reset-password")
    @ResponseBody
    public ResponseEntity<?> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        AppUser user = userRepository.findById(id).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        String newPassword = payload.getOrDefault("password", "");
        if (newPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Password cannot be empty."));
        }

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            return ResponseEntity.badRequest().body(Map.of("message", "New password cannot be the same as the current password."));
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        emailNotificationService.sendPasswordResetNotification(user.getFullName(), user.getEmail(), newPassword);
        return ResponseEntity.ok(Map.of("message", "Password reset for " + user.getEmail()));
    }

    @DeleteMapping("/api/admin/users/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteUser(@PathVariable Long id, Principal principal) {
        AppUser user = userRepository.findById(id).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();
        if (user.getEmail().equals(principal.getName())) {
            return ResponseEntity.badRequest().body(Map.of("message", "You cannot delete yourself."));
        }

        userRepository.delete(user);
        return ResponseEntity.ok(Map.of("message", "User " + user.getEmail() + " deleted."));
    }

    @PostMapping("/api/admin/change-password")
    @ResponseBody
    public ResponseEntity<?> changeOwnPassword(@RequestBody Map<String, String> payload, Principal principal) {
        String currentPassword = payload.getOrDefault("currentPassword", "");
        String newPassword = payload.getOrDefault("newPassword", "");

        if (currentPassword.isBlank() || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Both fields are required."));
        }

        AppUser admin = userRepository.findByEmail(principal.getName()).orElse(null);
        if (admin == null) return ResponseEntity.notFound().build();

        if (!passwordEncoder.matches(currentPassword, admin.getPasswordHash())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Current password is incorrect."));
        }

        if (passwordEncoder.matches(newPassword, admin.getPasswordHash())) {
            return ResponseEntity.badRequest().body(Map.of("message", "New password cannot be the same as the current password."));
        }

        admin.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(admin);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
    }
}
