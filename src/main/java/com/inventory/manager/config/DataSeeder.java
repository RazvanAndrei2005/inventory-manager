package com.inventory.manager.config;

import com.inventory.manager.entity.Role;
import com.inventory.manager.entity.User;
import com.inventory.manager.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_USERNAME}")
    private String adminUsername;

    @Value("${ADMIN_EMAIL}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD}")
    private String adminPassword;

    @Value("${MANAGER_USERNAME}")
    private String managerUsername;

    @Value("${MANAGER_EMAIL}")
    private String managerEmail;

    @Value("${MANAGER_PASSWORD}")
    private String managerPassword;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!userRepository.existsByUsername(adminUsername)) {
            User adminUser = new User();
            adminUser.setUsername(adminUsername);
            adminUser.setEmail(adminEmail);
            adminUser.setPassword(passwordEncoder.encode(adminPassword));
            adminUser.setRole(Role.ROLE_ADMIN);
            userRepository.save(adminUser);
        }

        if (!userRepository.existsByUsername(managerUsername)) {
            User managerUser = new User();
            managerUser.setUsername(managerUsername);
            managerUser.setEmail(managerEmail);
            managerUser.setPassword(passwordEncoder.encode(managerPassword));
            managerUser.setRole(Role.ROLE_MANAGER);
            userRepository.save(managerUser);
        }
    }
}
