package com.xirr.calculator.service;

import com.xirr.calculator.config.AppAccessRequestProperties;
import com.xirr.calculator.model.AccessRequestForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);
    private static final String BREVO_SEND_URL = "https://api.brevo.com/v3/smtp/email";

    private final AppAccessRequestProperties properties;
    private final String apiKey;
    private final String senderEmail;

    public EmailNotificationService(AppAccessRequestProperties properties,
                                    @Value("${BREVO_API_KEY:}") String apiKey,
                                    @Value("${BREVO_FROM_EMAIL:contact@kartikgupta.in}") String senderEmail) {
        this.properties = properties;
        this.apiKey = apiKey;
        this.senderEmail = senderEmail;
    }

    @Async
    public void sendAccessRequestNotification(AccessRequestForm form, String clientIp) {
        try {
            String htmlContent = "<p>New access request received:</p>"
                    + "<p><strong>Full Name:</strong> " + form.getFullName() + "</p>"
                    + "<p><strong>Email:</strong> " + form.getEmail() + "</p>"
                    + "<p><strong>Purpose:</strong> " + form.getPurpose() + "</p>"
                    + "<p><strong>Client IP:</strong> " + clientIp + "</p>";

            sendEmail(properties.notifyEmail(), "New Access Request: " + form.getEmail(), htmlContent);
        } catch (Exception e) {
            log.error("Failed to send access request notification email", e);
        }
    }

    @Async
    public void sendUserCreatedNotification(String fullName, String email, String password) {
        try {
            String htmlContent = "<p>New user created:</p>"
                    + "<p><strong>Full Name:</strong> " + fullName + "</p>"
                    + "<p><strong>Email:</strong> " + email + "</p>"
                    + "<p><strong>Password:</strong> " + password + "</p>";

            sendEmail(properties.notifyEmail(), "User Created: " + email, htmlContent);
        } catch (Exception e) {
            log.error("Failed to send user created notification email", e);
        }
    }

    @Async
    public void sendUserActivatedNotification(String fullName, String email) {
        try {
            String htmlContent = "<p>User activated:</p>"
                    + "<p><strong>Full Name:</strong> " + fullName + "</p>"
                    + "<p><strong>Email:</strong> " + email + "</p>";

            sendEmail(properties.notifyEmail(), "User Activated: " + email, htmlContent);
        } catch (Exception e) {
            log.error("Failed to send user activated notification email", e);
        }
    }

    @Async
    public void sendPasswordResetNotification(String fullName, String email, String newPassword) {
        try {
            String htmlContent = "<p>Password reset:</p>"
                    + "<p><strong>Full Name:</strong> " + fullName + "</p>"
                    + "<p><strong>Email:</strong> " + email + "</p>"
                    + "<p><strong>New Password:</strong> " + newPassword + "</p>";

            sendEmail(properties.notifyEmail(), "Password Reset: " + email, htmlContent);
        } catch (Exception e) {
            log.error("Failed to send password reset notification email", e);
        }
    }

    @Async
    public void sendResetCodeEmail(String email, String code) {
        try {
            String htmlContent = "<p>Your password reset verification code:</p>"
                    + "<h2 style='letter-spacing:4px;'>" + code + "</h2>"
                    + "<p>This code expires in 10 minutes.</p>"
                    + "<p>If you did not request this, ignore this email.</p>";

            sendEmail(email, "Password Reset Code", htmlContent);
        } catch (Exception e) {
            log.error("Failed to send reset code email", e);
        }
    }

    @Async
    public void sendAdminPasswordResetRequest(String fullName, String email) {
        try {
            String htmlContent = "<p>A user has requested a password reset:</p>"
                    + "<p><strong>Full Name:</strong> " + fullName + "</p>"
                    + "<p><strong>Email:</strong> " + email + "</p>"
                    + "<p>Please reset their password from the Admin Panel.</p>";

            sendEmail(properties.notifyEmail(), "Password Reset Request: " + email, htmlContent);
        } catch (Exception e) {
            log.error("Failed to send admin password reset request email", e);
        }
    }

    @Async
    public void sendReactivationRequest(String fullName, String email, String reason) {
        try {
            String htmlContent = "<p>A user has requested account reactivation:</p>"
                    + "<p><strong>Full Name:</strong> " + fullName + "</p>"
                    + "<p><strong>Email:</strong> " + email + "</p>"
                    + "<p><strong>Reason:</strong> " + reason + "</p>"
                    + "<p>Please reactivate from the Admin Panel if appropriate.</p>";

            sendEmail(properties.notifyEmail(), "Reactivation Request: " + email, htmlContent);
        } catch (Exception e) {
            log.error("Failed to send reactivation request email", e);
        }
    }

    private void sendEmail(String to, String subject, String htmlContent) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("BREVO_API_KEY not configured, skipping email: {}", subject);
            return;
        }

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", apiKey);

        Map<String, Object> body = Map.of(
                "sender", Map.of("email", senderEmail),
                "to", List.of(Map.of("email", to)),
                "subject", subject,
                "htmlContent", htmlContent
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(BREVO_SEND_URL, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("Brevo email request failed: status={} body={}", response.getStatusCodeValue(), response.getBody());
        }
    }
}
