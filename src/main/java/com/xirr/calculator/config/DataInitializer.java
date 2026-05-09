package com.xirr.calculator.config;

import com.xirr.calculator.model.AppUser;
import com.xirr.calculator.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail("contact@kartikgupta.in")) {
            AppUser admin = new AppUser(
                    "contact@kartikgupta.in",
                    "Kartik Gupta",
                    passwordEncoder.encode("ChangeThisNow!2026"),
                    true,
                    true,
                    Instant.now().plus(365 * 10, ChronoUnit.DAYS)
            );
            userRepository.save(admin);
            log.info("Default admin user created: contact@kartikgupta.in");
        }
    }
}
