package com.xirr.calculator.service;

import com.xirr.calculator.config.AppAccessRequestProperties;
import com.xirr.calculator.model.AccessRequestForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;
    private final AppAccessRequestProperties properties;
    private final String fromAddress;

    public EmailNotificationService(JavaMailSender mailSender,
                                    AppAccessRequestProperties properties,
                                    @Value("${spring.mail.username}") String fromAddress) {
        this.mailSender = mailSender;
        this.properties = properties;
        this.fromAddress = fromAddress;
    }

    @Async
    public void sendAccessRequestNotification(AccessRequestForm form, String clientIp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(properties.notifyEmail());
            message.setSubject("New Access Request: " + form.getDesiredUsername());
            message.setText(
                    "New access request received:\n\n" +
                    "Full Name: " + form.getFullName() + "\n" +
                    "Email: " + form.getEmail() + "\n" +
                    "Desired Username: " + form.getDesiredUsername() + "\n" +
                    "Purpose: " + form.getPurpose() + "\n" +
                    "Client IP: " + clientIp + "\n"
            );
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send access request notification email", e);
        }
    }
}
