package com.reliefgrid.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the first admin from BOOTSTRAP_ADMIN_EMAIL / BOOTSTRAP_ADMIN_PASSWORD when both are set. */
@Component
public class AdminBootstrap implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;

    public AdminBootstrap(UserRepository users, PasswordEncoder encoder,
                          @Value("${app.bootstrap-admin.email}") String email,
                          @Value("${app.bootstrap-admin.password}") String password) {
        this.users = users; this.encoder = encoder; this.email = email; this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank() || users.existsByEmail(email.toLowerCase())) return;
        User admin = new User();
        admin.setName("Administrator");
        admin.setEmail(email.toLowerCase());
        admin.setPasswordHash(encoder.encode(password));
        admin.setRole(Role.ADMIN);
        users.save(admin);
    }
}
