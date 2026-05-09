package com.barometer.crm;

import com.barometer.crm.model.User;
import com.barometer.crm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (userRepository.count() == 0) {
                User admin = new User();
                admin.setEmail("admin@barometer.com");
                admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
                admin.setName("Admin");
                admin.setRole("sales_head");
                admin.setAvatarInitials("AD");
                admin.setActive(true);
                userRepository.save(admin);

                log.info("╔══════════════════════════════════════════════╗");
                log.info("║  Default admin account created               ║");
                log.info("║  Email:    admin@barometer.com               ║");
                log.info("║  Password: Admin@123                         ║");
                log.info("║  Change the password after first login!      ║");
                log.info("╚══════════════════════════════════════════════╝");
            }
        } catch (Exception e) {
            log.warn("DataInitializer skipped — MongoDB not yet reachable: {}", e.getMessage());
        }
    }
}
