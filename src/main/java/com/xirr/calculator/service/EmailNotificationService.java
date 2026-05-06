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
                                    @Value("${BREVO_API_KEY}") String apiKey,
                                    @Value("${BREVO_FROM_EMAIL:contact@kartikgupta.in}") String senderEmail) {
        this.properties = properties;
        this.apiKey = apiKey;
        this.senderEmail = senderEmail;
    }

    @Async
    public void sendAccessRequestNotification(AccessRequestForm form, String clientIp) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);

            String htmlContent = "<p>New access request received:</p>"
                    + "<p><strong>Full Name:</strong> " + form.getFullName() + "</p>"
                    + "<p><strong>Email:</strong> " + form.getEmail() + "</p>"
                    + "<p><strong>Desired Username:</strong> " + form.getDesiredUsername() + "</p>"
                    + "<p><strong>Purpose:</strong> " + form.getPurpose() + "</p>"
                    + "<p><strong>Client IP:</strong> " + clientIp + "</p>";

            Map<String, Object> body = Map.of(
                    "sender", Map.of("email", senderEmail),
                    "to", List.of(Map.of("email", properties.notifyEmail())),
                    "subject", "New Access Request: " + form.getDesiredUsername(),
                    "htmlContent", htmlContent
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(BREVO_SEND_URL, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Brevo email request failed: status={} body={}", response.getStatusCodeValue(), response.getBody());
            }
        } catch (Exception e) {
            log.error("Failed to send access request notification email", e);
        }
    }
}
