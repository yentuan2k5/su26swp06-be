
// package com.swp391.scientific_journal_tracker.service;

// import com.resend.Resend;
// import com.resend.core.exception.ResendException;
// import com.resend.services.emails.model.CreateEmailOptions;
// import com.resend.services.emails.model.CreateEmailResponse;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Service;

// @Service
// public class EmailService {

//     @Value("${resend.api.key}")
//     private String resendApiKey;

//     @Value("${resend.from}")
//     private String fromEmail;

//     @Value("${app.frontend.url}")
//     private String frontendUrl;

//     public void sendResetPasswordEmail(String toEmail, String token) {
//         Resend resend = new Resend(resendApiKey);

//         String resetLink = frontendUrl + "/reset-password?token=" + token;

//         CreateEmailOptions params = CreateEmailOptions.builder()
//                 .from(fromEmail)
//                 .to(toEmail)
//                 .subject("Reset your password")
//                 .html("""
//                         <h2>Reset Password</h2>
//                         <p>Click the link below to reset your password:</p>
//                         <p><a href="%s">Reset Password</a></p>
//                         <p>If you did not request this, please ignore this email.</p>
//                         """.formatted(resetLink))
//                 .build();

//         try {
//             CreateEmailResponse response = resend.emails().send(params);
//             System.out.println("Resend email sent: " + response.getId());
//         } catch (ResendException e) {
//             throw new RuntimeException("Không gửi được email reset password bằng Resend", e);
//         }
//     }
// }
package com.swp391.scientific_journal_tracker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    @Value("${brevo.from.email}")
    private String fromEmail;

    @Value("${brevo.from.name}")
    private String fromName;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public void sendResetPasswordEmail(String toEmail, String token) {
        String resetLink = frontendUrl.replaceAll("/$", "") + "/reset-password?token=" + token;

        String htmlContent = """
                <h2>Reset Password</h2>
                <p>You requested to reset your password.</p>
                <p>Click the link below to reset your password:</p>
                <p><a href="%s">Reset Password</a></p>
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
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            System.out.println("Brevo email sent: " + response.getBody());
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException(
                    "Brevo gửi mail lỗi: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(),
                    e);
        } catch (RestClientException e) {
            throw new RuntimeException("Không gửi được email reset password bằng Brevo", e);
        }
    }
}
