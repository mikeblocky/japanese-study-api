package com.japanesestudy.app.util;

import com.japanesestudy.app.entity.Role;
import com.japanesestudy.app.entity.User;
import com.japanesestudy.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            log.info("🌱 Seeding default users...");

            // Create Admin using builder pattern
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin"))
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin);

            // Create User using builder pattern
            User user = User.builder()
                    .username("user")
                    .password(passwordEncoder.encode("user"))
                    .role(Role.USER)
                    .build();
            userRepository.save(user);

            log.info("✅ Default users created (admin/admin, user/user)");
        }
    }
}
