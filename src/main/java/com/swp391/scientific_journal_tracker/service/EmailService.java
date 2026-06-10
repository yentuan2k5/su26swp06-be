package com.swp391.scientific_journal_tracker.service;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor

public class EmailService {

    private final JavaMailSender mailSender;
    @Value("${spring.mail.from}")
    private String fromEmail;

    public void sendResetPasswordEmail(String toEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Đặt lại mật khẩu");
        message.setText("Bạn vừa yêu cầu đặt lại mật khẩu.\n\n"
                + "Click vào link sau (hết hạn sau 15 phút):\n"
                + resetLink
                + "\n\nNếu không phải bạn, hãy bỏ qua email này.");
        message.setFrom(fromEmail);
        mailSender.send(message);

    }
}
