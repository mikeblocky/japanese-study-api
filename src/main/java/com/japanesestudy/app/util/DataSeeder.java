package com.japanesestudy.app.util;

import com.japanesestudy.app.model.User;
import com.japanesestudy.app.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;

    public DataSeeder(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            System.out.println("🌱 Seeding default user...");

            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword("admin"); // In a real app, hash this
            admin.setEmail("admin@example.com");
            admin.setRole("ADMIN");

            userRepository.save(admin);

            System.out.println("✅ Default user created with ID: " + admin.getId());
        }
    }
}
