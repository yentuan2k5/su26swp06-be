// package com.swp391.scientific_journal_tracker.service;

// import lombok.RequiredArgsConstructor;

// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.mail.SimpleMailMessage;
// import org.springframework.mail.javamail.JavaMailSender;
// import org.springframework.stereotype.Service;

// @Service
// @RequiredArgsConstructor

// public class EmailService {

//     private final JavaMailSender mailSender;
//     @Value("${spring.mail.from}")
//     private String fromEmail;

//     public void sendResetPasswordEmail(String toEmail, String resetLink) {
//         SimpleMailMessage message = new SimpleMailMessage();
//         message.setTo(toEmail);
//         message.setSubject("Đặt lại mật khẩu");
//         message.setText("Bạn vừa yêu cầu đặt lại mật khẩu.\n\n"
//                 + "Click vào link sau (hết hạn sau 15 phút):\n"
//                 + resetLink
//                 + "\n\nNếu không phải bạn, hãy bỏ qua email này.");
//         message.setFrom(fromEmail);
//         mailSender.send(message);

//     }
// }
package com.swp391.scientific_journal_tracker.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Value("${resend.api.key}")
    private String resendApiKey;

    @Value("${resend.from}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public void sendResetPasswordEmail(String toEmail, String token) {
        Resend resend = new Resend(resendApiKey);

        String resetLink = frontendUrl + "/reset-password?token=" + token;

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(fromEmail)
                .to(toEmail)
                .subject("Reset your password")
                .html("""
                        <h2>Reset Password</h2>
                        <p>Click the link below to reset your password:</p>
                        <p><a href="%s">Reset Password</a></p>
                        <p>If you did not request this, please ignore this email.</p>
                        """.formatted(resetLink))
                .build();

        try {
            CreateEmailResponse response = resend.emails().send(params);
            System.out.println("Resend email sent: " + response.getId());
        } catch (ResendException e) {
            throw new RuntimeException("Không gửi được email reset password bằng Resend", e);
        }
    }
}
