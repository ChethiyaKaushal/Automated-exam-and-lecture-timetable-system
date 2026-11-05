package com.university.exam_lecture_timetable.config;

import com.university.exam_lecture_timetable.entity.RoleEntity;
import com.university.exam_lecture_timetable.entity.RoleName;
import com.university.exam_lecture_timetable.entity.UserEntity;
import com.university.exam_lecture_timetable.repository.RoleRepository;
import com.university.exam_lecture_timetable.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Set;

import static com.university.exam_lecture_timetable.entity.RoleName.EXAM_TIMETABLE_OFFICER;

@Component
public class DataInitializer implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.initial-admin.email:admin@university.edu}")
    private String adminEmail;

    @Value("${app.security.initial-admin.password:Admin@12345}")
    private String adminPassword;

    @Value("${app.security.initial-admin.name:System Administrator}")
    private String adminName;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        initializeRoles();
        initializeAdminUser();
    }

    private void initializeRoles() {
        Arrays.stream(RoleName.values()).forEach(roleName -> {
            if (!roleRepository.existsByName(roleName)) {
                String description = switch (roleName) {
                    case ADMIN -> "Full system access and user management authority";
                    case EXAM_TIMETABLE_OFFICER -> "Authorized to manage examination timetables";
                    case LECTURER_TIMETABLE_OFFICER -> "Authorized to manage lecturer and lecture timetables";
                    case LECTURER -> "Can view published timetables and submit timetable issues/requests";
                    case STUDENT -> "Can view relevant published timetables only";
                };
                roleRepository.save(new RoleEntity(roleName, description));
                logger.info("Initialized role: {}", roleName);
            }
        });
    }

    private void initializeAdminUser() {
        if (!userRepository.existsByEmail(adminEmail)) {
            RoleEntity adminRole = roleRepository.findByName(RoleName.ADMIN)
                    .orElseThrow(() -> new IllegalStateException("ADMIN role not initialized"));

            UserEntity admin = new UserEntity();
            admin.setFullName(adminName);
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setEnabled(true);
            admin.setAccountNonLocked(true);
            admin.setMustChangePassword(true);
            admin.setRoles(Set.of(adminRole));

            userRepository.save(admin);
            logger.info("Initialized default administrator account: {}", adminEmail);
        }
    }
}
