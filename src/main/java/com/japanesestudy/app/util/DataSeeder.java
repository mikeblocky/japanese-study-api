package com.japanesestudy.app.util;

import com.japanesestudy.app.entity.Role;
import com.japanesestudy.app.entity.User;
import com.japanesestudy.app.repository.CourseRepository;
import com.japanesestudy.app.repository.TopicRepository;
import com.japanesestudy.app.repository.StudyItemRepository;
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

    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("Seeding default admin user...");
            User admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin"))
                .role(Role.ADMIN)
                .build();
            userRepository.save(admin);
            log.info("Default admin created (admin/admin)");
        }
    }
}
