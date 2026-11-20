package com.finguard.auth.config;

import com.finguard.auth.entity.Role;
import com.finguard.auth.entity.User;
import com.finguard.auth.repository.RoleRepository;
import com.finguard.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * Automatically seeds development accounts when running with SPRING_PROFILES_ACTIVE=dev.
 * Isolated from production environments to prevent shared default credentials in deployed environments.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("Active profile is 'dev': checking seed accounts...");

        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElse(null);
        Role analystRole = roleRepository.findByName("ROLE_ANALYST").orElse(null);
        Role userRole = roleRepository.findByName("ROLE_USER").orElse(null);

        if (adminRole == null || analystRole == null || userRole == null) {
            log.warn("Core roles not yet provisioned. Skipping dev seed.");
            return;
        }

        seedUserIfNotExists("dev_admin", "dev_admin@finguard.local", "DevAdmin@123", Set.of(adminRole));
        seedUserIfNotExists("dev_analyst", "dev_analyst@finguard.local", "DevAnalyst@123", Set.of(analystRole));
        seedUserIfNotExists("dev_user", "dev_user@finguard.local", "DevUser@123", Set.of(userRole));
    }

    private void seedUserIfNotExists(String username, String email, String rawPassword, Set<Role> roles) {
        if (!userRepository.existsByUsername(username)) {
            User user = User.builder()
                    .username(username)
                    .email(email)
                    .passwordHash(passwordEncoder.encode(rawPassword))
                    .firstName("Dev")
                    .lastName("Account")
                    .isEnabled(true)
                    .failedLoginAttempts(0)
                    .roles(roles)
                    .build();
            userRepository.save(user);
            log.info("Provisioned dev account: username={}, password={}", username, rawPassword);
        }
    }
}
