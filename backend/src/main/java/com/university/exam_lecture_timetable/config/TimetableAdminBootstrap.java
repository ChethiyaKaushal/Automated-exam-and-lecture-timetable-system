package com.university.exam_lecture_timetable.config;

import com.university.exam_lecture_timetable.entity.UserEntity;
import com.university.exam_lecture_timetable.entity.RoleEntity;
import com.university.exam_lecture_timetable.entity.RoleName;
import com.university.exam_lecture_timetable.repository.UserRepository;
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

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final String fullName;
    private final String email;
    private final String password;

    public TimetableAdminBootstrap(
            UserRepository users, RoleRepository roles, PasswordEncoder encoder,
            @Value("${timetable-admin.create}") String fullName,
            @Value("${timetable-admin.email}") String email,
            @Value("${TIMETABLE_ADMIN_PASSWORD:}") String password
    ) {
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
        this.fullName = fullName;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (fullName.isBlank() || !email.contains("@") || password.length() < 12
                || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Admin setup requires a name, email, and a password of at least 12 characters and at most 72 UTF-8 bytes.");
        }
        if (users.existsByEmail(email)) {
            throw new IllegalStateException("Admin setup will not replace an existing account. Remove the setup arguments.");
        }

        var role = roles.findByName(RoleName.ADMIN).orElseGet(() -> {
            var newRole = new RoleEntity();
            newRole.setName(RoleName.ADMIN);
            newRole.setDescription("Exam timetable administrator");
            return roles.save(newRole);
        });

        var user = new UserEntity(fullName, email, encoder.encode(password));
        user.addRole(role);
        users.save(user);
        LoggerFactory.getLogger(TimetableAdminBootstrap.class)
                .info("Timetable admin account created. Remove the admin setup arguments before the next startup.");
    }
}
