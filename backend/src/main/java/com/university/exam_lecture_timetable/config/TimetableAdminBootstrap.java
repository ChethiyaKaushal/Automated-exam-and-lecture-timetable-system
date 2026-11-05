package com.university.exam_lecture_timetable.config;

import com.university.exam_lecture_timetable.entity.AppUserEntity;
import com.university.exam_lecture_timetable.entity.RoleEntity;
import com.university.exam_lecture_timetable.entity.RoleName;
import com.university.exam_lecture_timetable.repository.AppUserRepository;
import com.university.exam_lecture_timetable.repository.RoleRepository;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "timetable-admin.create")
public class TimetableAdminBootstrap implements ApplicationRunner {

    private final AppUserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final String username;
    private final String email;
    private final String password;

    public TimetableAdminBootstrap(
            AppUserRepository users, RoleRepository roles, PasswordEncoder encoder,
            @Value("${timetable-admin.create}") String username,
            @Value("${timetable-admin.email}") String email,
            @Value("${TIMETABLE_ADMIN_PASSWORD:}") String password
    ) {
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (username.isBlank() || !email.contains("@") || password.length() < 12
                || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Admin setup requires a username, email, and a password of at least 12 characters and at most 72 UTF-8 bytes.");
        }
        if (users.existsByUsername(username) || users.existsByEmail(email)) {
            throw new IllegalStateException("Admin setup will not replace an existing account. Remove the setup arguments.");
        }
//        var role = roles.findByName("ADMIN").orElseGet(() -> {
//            var newRole = new RoleEntity();
//            newRole.setName("ADMIN");
//            newRole.setDescription("Exam timetable administrator");
//            return roles.save(newRole);
//        });
        var role = roles.findByName(RoleName.ADMIN).orElseGet(() -> {
            var newRole = new RoleEntity();
            newRole.setName(RoleName.ADMIN);
            newRole.setDescription("Exam timetable administrator");
            return roles.save(newRole);
        });

        var user = new AppUserEntity();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(password));
        user.getRoles().add(role);
        users.save(user);
        LoggerFactory.getLogger(TimetableAdminBootstrap.class)
                .info("Timetable admin account created. Remove the admin setup arguments before the next startup.");
    }
}
