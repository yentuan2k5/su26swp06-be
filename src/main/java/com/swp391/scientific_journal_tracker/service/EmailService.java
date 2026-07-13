package com.swp391.scientific_journal_tracker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.swp391.scientific_journal_tracker.exception.EmailSendingException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final RestTemplate restTemplate;

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    @Value("${brevo.from.email}")
    private String fromEmail;

    @Value("${brevo.from.name}")
    private String fromName;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public void sendResetPasswordEmail(String toEmail, String token) {
        String encodedToken = URLEncoder.encode(
                token,
                StandardCharsets.UTF_8);

        String normalizedFrontendUrl = frontendUrl.replaceAll("/$", "");

        String resetLink = normalizedFrontendUrl
                + "/reset-password?token="
                + encodedToken;

        String htmlContent = """
                <h2>Reset Password</h2>
                <p>You requested to reset your password.</p>
                <p>Click the link below to reset your password:</p>
                <p>
                    <a href="%s">Reset Password</a>
                </p>
                <p>This link will expire soon.</p>
                <p>If you did not request this, please ignore this email.</p>
                """.formatted(resetLink);

        String url = "https://api.brevo.com/v3/smtp/email";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", brevoApiKey);

        Map<String, Object> sender = Map.of(
                "name", fromName,
                "email", fromEmail);

        Map<String, Object> receiver = Map.of(
                "email", toEmail);

        Map<String, Object> body = new HashMap<>();
        body.put("sender", sender);
        body.put("to", List.of(receiver));
        body.put("subject", "Reset your password");
        body.put("htmlContent", htmlContent);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    url,
                    request,
                    String.class);

            log.info(
                    "Reset password email sent successfully to {} with status {}",
                    maskEmail(toEmail),
                    response.getStatusCode());
        } catch (HttpStatusCodeException e) {
            log.error(
                    "Brevo rejected reset password email request. Status: {}, response: {}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString());

            throw new EmailSendingException(
                    "Unable to send reset password email",
                    e);
        } catch (RestClientException e) {
            log.error(
                    "Failed to connect to Brevo while sending reset password email",
                    e);

            throw new EmailSendingException(
                    "Unable to send reset password email",
                    e);
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }

        String[] parts = email.split("@", 2);
        String username = parts[0];
        String domain = parts[1];

        String maskedUsername = username.length() <= 2
                ? "***"
                : username.substring(0, 2) + "***";

        return maskedUsername + "@" + domain;
    }
}