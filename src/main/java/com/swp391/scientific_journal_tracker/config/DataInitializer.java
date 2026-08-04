package com.swp391.scientific_journal_tracker.config;

import com.swp391.scientific_journal_tracker.entity.User;
import com.swp391.scientific_journal_tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.enabled:false}")
    private boolean adminInitializationEnabled;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        List<User> existingAdmins = userRepository.findByRole(User.Role.ADMIN);

        if (existingAdmins.size() > 1) {
            throw new IllegalStateException(
                    "Dữ liệu không hợp lệ: hệ thống chỉ được có một tài khoản ADMIN");
        }

        if (!adminInitializationEnabled) {
            log.info("Default admin initialization is disabled");
            return;
        }

        validateAdminConfiguration();

        if (!existingAdmins.isEmpty()) {
            log.info("A system admin account already exists");
            return;
        }

        String normalizedAdminEmail = adminEmail.trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedAdminEmail)) {
            throw new IllegalStateException(
                    "ADMIN_EMAIL đang thuộc về một tài khoản không phải ADMIN");
        }

        User admin = new User();
        admin.setUsername("System Admin");
        admin.setEmail(normalizedAdminEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(User.Role.ADMIN);
        admin.setProvider("local");

        userRepository.save(admin);

        log.info("Default admin account created successfully");
    }

    private void validateAdminConfiguration() {
        if (adminEmail == null || adminEmail.isBlank()) {
            throw new IllegalStateException(
                    "ADMIN_EMAIL must be configured when admin initialization is enabled");
        }

        if (adminPassword == null || adminPassword.length() < 12) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD must contain at least 12 characters");
        }
    }
}
