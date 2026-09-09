package com.example.lifecapsule.config;

import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.entity.enumirated.Role;
import com.example.lifecapsule.entity.enumirated.Status;
import com.example.lifecapsule.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.admin.enabled", havingValue = "true", matchIfMissing = true)
public class AdminUserInitializer implements ApplicationRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;
    private final String email;
    private final String firstName;
    private final String lastName;

    public AdminUserInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.username:admin}") String username,
            @Value("${app.admin.password:Admin123!}") String password,
            @Value("${app.admin.email:admin@lifecapsule.local}") String email,
            @Value("${app.admin.first-name:Admin}") String firstName,
            @Value("${app.admin.last-name:User}") String lastName) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String normalizedUsername = username == null ? "" : username.trim();
        String normalizedPassword = password == null ? "" : password.trim();
        if (normalizedUsername.isBlank() || normalizedPassword.isBlank()) {
            return;
        }

        Users user = userRepository.findByUserNameIgnoreCase(normalizedUsername).orElseGet(Users::new);
        boolean isNewUser = user.getId() == null;

        user.setUserName(normalizedUsername);
        if (isNewUser) {
            user.setPassword(passwordEncoder.encode(normalizedPassword));
            user.setEmail(resolveAdminEmail(normalizedUsername));
        } else if (user.getEmail() == null || user.getEmail().isBlank()) {
            user.setEmail(resolveAdminEmail(normalizedUsername));
        }

        if (user.getFirstName() == null || user.getFirstName().isBlank()) {
            user.setFirstName(firstName);
        }
        if (user.getLastName() == null || user.getLastName().isBlank()) {
            user.setLastName(lastName);
        }
        user.setRole(Role.ADMIN);
        user.setStatus(Status.ACTIVE);

        userRepository.save(user);
    }

    private String resolveAdminEmail(String normalizedUsername) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        if (normalizedEmail.isBlank()) {
            normalizedEmail = normalizedUsername.toLowerCase() + "@lifecapsule.local";
        }
        if (!userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            return normalizedEmail;
        }
        return normalizedUsername.toLowerCase() + "-admin@lifecapsule.local";
    }
}
