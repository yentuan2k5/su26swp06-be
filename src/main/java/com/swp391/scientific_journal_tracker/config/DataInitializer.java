package com.swp391.scientific_journal_tracker.config;

import com.swp391.scientific_journal_tracker.entity.User;
import com.swp391.scientific_journal_tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

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
        if (!adminInitializationEnabled) {
            log.info("Default admin initialization is disabled");
            return;
        }

        validateAdminConfiguration();

        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Admin account already exists");
            return;
        }

        User admin = new User();
        admin.setUsername("System Admin");
        admin.setEmail(adminEmail.trim().toLowerCase());
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