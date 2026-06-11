package org.project.config;

import org.project.model.AppUser;
import org.project.model.UserRole;
import org.project.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner createDefaultAdmin(UserRepository userRepository,
                                         PasswordEncoder passwordEncoder,
                                         @Value("${app.admin.email:}") String configuredAdminEmail,
                                         @Value("${app.admin.password:}") String configuredAdminPassword) {
        return args -> {
            if (!userRepository.existsByEmailIgnoreCase("admin@roomallocation.local")) {
                userRepository.save(new AppUser(
                        "admin@roomallocation.local",
                        passwordEncoder.encode("admin123"),
                        UserRole.ADMIN
                ));
            }

            if (!configuredAdminEmail.isBlank() && !configuredAdminPassword.isBlank()) {
                AppUser admin = userRepository.findByEmailIgnoreCase(configuredAdminEmail)
                        .orElseGet(() -> new AppUser(configuredAdminEmail, "", UserRole.ADMIN));
                admin.setRole(UserRole.ADMIN);
                admin.setPassword(passwordEncoder.encode(configuredAdminPassword));
                admin.setEnabled(true);
                userRepository.save(admin);
            }
        };
    }
}
