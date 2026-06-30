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

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);
    private static final String BREVO_SEND_URL = "https://api.brevo.com/v3/smtp/email";

    private final AppAccessRequestProperties properties;
    private final String apiKey;
    private final String senderEmail;
    private final String siteUrl;

    public EmailNotificationService(AppAccessRequestProperties properties,
                                    @Value("${BREVO_API_KEY:}") String apiKey,
                                    @Value("${BREVO_FROM_EMAIL:contact@kartikgupta.in}") String senderEmail,
                                    @Value("${SITE_URL:http://localhost:8080}") String siteUrl) {
        this.properties = properties;
        this.apiKey = apiKey;
        this.senderEmail = senderEmail;
        this.siteUrl = siteUrl;
    }

    @Async
    public void sendAccessRequestNotification(AccessRequestForm form, String clientIp) {
        try {
            String html = wrap("New Access Request",
                    "<p>A new access request has been submitted:</p>"
                    + field("Full Name", form.getFullName())
                    + field("Email", form.getEmail())
                    + field("Purpose", form.getPurpose())
                    + field("Client IP", clientIp));

            sendEmail(properties.notifyEmail(), "New Access Request: " + form.getEmail(), html);
        } catch (Exception e) {
            log.error("Failed to send access request notification email", e);
        }
    }

    @Async
    public void sendUserCreatedNotification(String fullName, String email, String password, Instant expiresAt) {
        try {
            String expiryFormatted = ZonedDateTime.ofInstant(expiresAt, ZoneId.of("Asia/Kolkata"))
                    .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));

            String userHtml = wrap("Welcome to XIRR Calculator!",
                    "<p>Hi <strong>" + fullName + "</strong>,</p>"
                    + "<p>Your account has been created. Here are your login details:</p>"
                    + card(field("Login Email", email) + field("Password", password) + field("Access Expires", expiryFormatted))
                    + button("Log In Now", siteUrl)
                    + "<p style='margin-top:1rem;'>We recommend changing your password after your first login for security.</p>"
                    + "<p style='color:#6b7280;font-size:13px;margin-top:1.5rem;'>If you did not request this account, please ignore this email.</p>");

            sendEmail(email, "Welcome to XIRR Calculator", userHtml);

            String adminHtml = wrap("New User Created",
                    "<p>A new user account has been created:</p>"
                    + field("Full Name", fullName)
                    + field("Email", email)
                    + field("Password", password)
                    + field("Expires At", expiryFormatted));

            sendEmail(properties.notifyEmail(), "User Created: " + email, adminHtml);
        } catch (Exception e) {
            log.error("Failed to send user created notification email", e);
        }
    }

    @Async
    public void sendUserActivatedNotification(String fullName, String email) {
        try {
            String userHtml = wrap("Account Activated",
                    "<p>Hi <strong>" + fullName + "</strong>,</p>"
                    + "<p>Great news! Your XIRR Calculator account has been activated by the administrator.</p>"
                    + button("Log In Now", siteUrl)
                    + "<p style='color:#6b7280;font-size:13px;margin-top:1.5rem;'>If you have any questions, contact the administrator.</p>");

            sendEmail(email, "Your Account Has Been Activated", userHtml);

            String adminHtml = wrap("User Activated",
                    "<p>The following user has been activated:</p>"
                    + field("Full Name", fullName)
                    + field("Email", email));

            sendEmail(properties.notifyEmail(), "User Activated: " + email, adminHtml);
        } catch (Exception e) {
            log.error("Failed to send user activated notification email", e);
        }
    }

    @Async
    public void sendPasswordResetNotification(String fullName, String email, String newPassword) {
        try {
            String userHtml = wrap("Password Reset",
                    "<p>Hi <strong>" + fullName + "</strong>,</p>"
                    + "<p>Your password has been reset by the administrator. Here are your new credentials:</p>"
                    + card(field("Email", email) + field("New Password", newPassword))
                    + button("Log In Now", siteUrl)
                    + "<p style='margin-top:1rem;'>Please change your password after logging in for security.</p>");

            sendEmail(email, "Your Password Has Been Reset", userHtml);

            String adminHtml = wrap("Password Reset Completed",
                    "<p>Password has been reset for:</p>"
                    + field("Full Name", fullName)
                    + field("Email", email)
                    + field("New Password", newPassword));

            sendEmail(properties.notifyEmail(), "Password Reset: " + email, adminHtml);
        } catch (Exception e) {
            log.error("Failed to send password reset notification email", e);
        }
    }

    @Async
    public void sendResetCodeEmail(String email, String code) {
        try {
            String userHtml = wrap("Verification Code",
                    "<p>You requested a password reset. Use the code below to verify your identity:</p>"
                    + "<div style='text-align:center;margin:1.5rem 0;'>"
                    + "<span style='display:inline-block;padding:16px 32px;background:#f8fafc;border:2px dashed #ea580c;border-radius:12px;font-size:28px;font-weight:700;letter-spacing:8px;color:#1f2937;'>" + code + "</span>"
                    + "</div>"
                    + "<p>This code expires in <strong>10 minutes</strong>.</p>"
                    + "<p style='color:#6b7280;font-size:13px;margin-top:1.5rem;'>If you did not request this, ignore this email.</p>");

            sendEmail(email, "Password Reset Code: " + code, userHtml);

            String adminHtml = wrap("Password Reset Code Sent",
                    "<p>A password reset verification code was sent to:</p>"
                    + field("Email", email)
                    + field("Code", code)
                    + "<p style='color:#6b7280;font-size:13px;margin-top:1rem;'>This is for your records.</p>");

            sendEmail(properties.notifyEmail(), "Reset Code Sent: " + email, adminHtml);
        } catch (Exception e) {
            log.error("Failed to send reset code email", e);
        }
    }

    @Async
    public void sendAdminPasswordResetRequest(String fullName, String email) {
        try {
            String html = wrap("Password Reset Request",
                    "<p>A user has requested a password reset:</p>"
                    + field("Full Name", fullName)
                    + field("Email", email)
                    + "<p style='margin-top:1rem;'>Please reset their password from the <strong>Admin Panel</strong>.</p>");

            sendEmail(properties.notifyEmail(), "Password Reset Request: " + email, html);
        } catch (Exception e) {
            log.error("Failed to send admin password reset request email", e);
        }
    }

    @Async
    public void sendXirrResultNotification(String fullName, String email,
                                           String formattedXirr,
                                           int transactionCount,
                                           java.math.BigDecimal totalInvested,
                                           java.math.BigDecimal totalRedeemed,
                                           java.math.BigDecimal profitOrLoss) {
        try {
            boolean isPositive = profitOrLoss.signum() >= 0;
            String xirrColor = isPositive ? "#16a34a" : "#dc2626";
            String profitColor = isPositive ? "#16a34a" : "#dc2626";
            String profitLabel = isPositive ? "Profit" : "Loss";

            String xirrHighlight = "<div style='text-align:center;margin:24px 0;'>"
                    + "<div style='display:inline-block;padding:20px 40px;background:linear-gradient(135deg,"
                    + (isPositive ? "#f0fdf4,#dcfce7" : "#fef2f2,#fee2e2")
                    + ");border-radius:16px;border:2px solid " + (isPositive ? "#bbf7d0" : "#fecaca") + ";'>"
                    + "<p style='margin:0 0 4px;font-size:13px;color:#6b7280;font-weight:500;'>Your XIRR Return</p>"
                    + "<p style='margin:0;font-size:36px;font-weight:800;color:" + xirrColor + ";letter-spacing:-1px;'>" + formattedXirr + "</p>"
                    + "</div></div>";

            String summaryCard = "<div style='margin:20px 0;padding:20px 24px;background:#f8fafc;border-radius:12px;border:1px solid #e2e8f0;'>"
                    + "<table style='width:100%;border-collapse:collapse;font-size:14px;'>"
                    + "<tr><td style='padding:8px 0;color:#6b7280;'>Transactions Processed</td><td style='padding:8px 0;text-align:right;font-weight:600;color:#1f2937;'>" + transactionCount + "</td></tr>"
                    + "<tr><td style='padding:8px 0;border-top:1px solid #e2e8f0;color:#6b7280;'>Total Invested</td><td style='padding:8px 0;border-top:1px solid #e2e8f0;text-align:right;font-weight:600;color:#1f2937;'>" + formatCurrency(totalInvested) + "</td></tr>"
                    + "<tr><td style='padding:8px 0;border-top:1px solid #e2e8f0;color:#6b7280;'>Total Redeemed</td><td style='padding:8px 0;border-top:1px solid #e2e8f0;text-align:right;font-weight:600;color:#1f2937;'>" + formatCurrency(totalRedeemed) + "</td></tr>"
                    + "<tr><td style='padding:8px 0;border-top:1px solid #e2e8f0;color:#6b7280;'>" + profitLabel + "</td><td style='padding:8px 0;border-top:1px solid #e2e8f0;text-align:right;font-weight:700;color:" + profitColor + ";'>" + (isPositive ? "+" : "") + formatCurrency(profitOrLoss) + "</td></tr>"
                    + "</table></div>";

            String timestamp = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))
                    .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));

            String userHtml = wrap("Your XIRR Calculation Result",
                    "<p>Hi <strong>" + fullName + "</strong>,</p>"
                    + "<p>Your XIRR has been calculated successfully. Here's a summary of your investment performance:</p>"
                    + xirrHighlight
                    + summaryCard
                    + "<p style='font-size:12px;color:#9ca3af;margin-top:16px;'>Calculated on " + timestamp + " IST</p>"
                    + button("View Details", siteUrl)
                    + "<p style='color:#6b7280;font-size:13px;margin-top:1.5rem;'>This is an automated notification from XIRR Calculator. The XIRR value represents the annualized return on your investments.</p>");

            sendEmail(email, "Your XIRR: " + formattedXirr + " \u2014 Calculation Complete", userHtml);
        } catch (Exception e) {
            log.error("Failed to send XIRR result notification email to {}", email, e);
        }
    }

    private String formatCurrency(java.math.BigDecimal amount) {
        java.text.NumberFormat formatter = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("en", "IN"));
        return formatter.format(amount);
    }

    @Async
    public void sendReactivationRequest(String fullName, String email, String reason) {
        try {
            String html = wrap("Reactivation Request",
                    "<p>A user has requested account reactivation:</p>"
                    + field("Full Name", fullName)
                    + field("Email", email)
                    + field("Reason", reason)
                    + "<p style='margin-top:1rem;'>Please reactivate from the <strong>Admin Panel</strong> if appropriate.</p>");

            sendEmail(properties.notifyEmail(), "Reactivation Request: " + email, html);
        } catch (Exception e) {
            log.error("Failed to send reactivation request email", e);
        }
    }

    @Async
    public void sendUserDeactivatedNotification(String fullName, String email) {
        try {
            String userHtml = wrap("Account Deactivated",
                    "<p>Hi <strong>" + fullName + "</strong>,</p>"
                    + "<p>Your XIRR Calculator account has been deactivated by the administrator.</p>"
                    + "<p>You will no longer be able to log in until your account is reactivated.</p>"
                    + "<p style='color:#6b7280;font-size:13px;margin-top:1.5rem;'>If you believe this is a mistake, please contact the administrator.</p>");

            sendEmail(email, "Your Account Has Been Deactivated", userHtml);

            String adminHtml = wrap("User Deactivated",
                    "<p>The following user has been deactivated:</p>"
                    + field("Full Name", fullName)
                    + field("Email", email));

            sendEmail(properties.notifyEmail(), "User Deactivated: " + email, adminHtml);
        } catch (Exception e) {
            log.error("Failed to send user deactivated notification email", e);
        }
    }

    @Async
    public void sendExpiryUpdatedNotification(String fullName, String email, Instant newExpiry) {
        try {
            String expiryFormatted = ZonedDateTime.ofInstant(newExpiry, ZoneId.of("Asia/Kolkata"))
                    .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));

            String userHtml = wrap("Access Extended",
                    "<p>Hi <strong>" + fullName + "</strong>,</p>"
                    + "<p>As per your request, your login access has been extended.</p>"
                    + card(field("New Expiry", expiryFormatted))
                    + button("Log In Now", siteUrl)
                    + "<p style='color:#6b7280;font-size:13px;margin-top:1.5rem;'>After this time, you will need to request reactivation.</p>");

            sendEmail(email, "Your Access Has Been Extended", userHtml);

            String adminHtml = wrap("Expiry Updated",
                    "<p>Login expiry has been updated for:</p>"
                    + field("Full Name", fullName)
                    + field("Email", email)
                    + field("New Expiry", expiryFormatted));

            sendEmail(properties.notifyEmail(), "Expiry Updated: " + email, adminHtml);
        } catch (Exception e) {
            log.error("Failed to send expiry updated notification email", e);
        }
    }

    @Async
    public void sendUserDeletedNotification(String fullName, String email) {
        try {
            String userHtml = wrap("Account Removed",
                    "<p>Hi <strong>" + fullName + "</strong>,</p>"
                    + "<p>As per your request, your XIRR Calculator account has been permanently removed.</p>"
                    + "<p>All your account data has been deleted. If you need access again in the future, you can submit a new access request from the login page.</p>"
                    + "<p style='color:#6b7280;font-size:13px;margin-top:1.5rem;'>Thank you for using XIRR Calculator.</p>");

            sendEmail(email, "Your Account Has Been Removed", userHtml);

            String adminHtml = wrap("User Deleted",
                    "<p>The following user has been permanently deleted:</p>"
                    + field("Full Name", fullName)
                    + field("Email", email));

            sendEmail(properties.notifyEmail(), "User Deleted: " + email, adminHtml);
        } catch (Exception e) {
            log.error("Failed to send user deleted notification email", e);
        }
    }

    // --- Email template helpers ---

    private String wrap(String title, String body) {
        return "<div style='font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,sans-serif;max-width:560px;margin:0 auto;padding:32px 24px;'>"
                + "<div style='border-radius:16px;border:1px solid #e2e8f0;padding:32px;background:#ffffff;'>"
                + "<h2 style='margin:0 0 20px;font-size:22px;color:#1f2937;'>" + title + "</h2>"
                + body
                + "</div>"
                + "<p style='text-align:center;margin-top:20px;font-size:12px;color:#9ca3af;'>XIRR Calculator &mdash; kartikgupta.in</p>"
                + "</div>";
    }

    private String field(String label, String value) {
        return "<p style='margin:6px 0;font-size:14px;'><span style='color:#6b7280;'>" + label + ":</span> <strong style='color:#1f2937;'>" + value + "</strong></p>";
    }

    private String card(String content) {
        return "<div style='margin:16px 0;padding:16px 20px;background:#f8fafc;border-radius:12px;border:1px solid #e2e8f0;'>" + content + "</div>";
    }

    private String button(String text, String url) {
        return "<div style='text-align:center;margin:20px 0;'>"
                + "<a href='" + url + "' style='display:inline-block;padding:12px 28px;background:linear-gradient(135deg,#ea580c,#be123c);color:white;text-decoration:none;border-radius:999px;font-weight:700;font-size:14px;'>" + text + " &rarr;</a>"
                + "</div>";
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
