package com.university.exam_lecture_timetable.service.impl;

import com.university.exam_lecture_timetable.dto.request.CreateUserRequest;
import com.university.exam_lecture_timetable.dto.request.UpdateUserRequest;
import com.university.exam_lecture_timetable.dto.request.UpdateUserRolesRequest;
import com.university.exam_lecture_timetable.dto.request.UpdateUserStatusRequest;
import com.university.exam_lecture_timetable.dto.response.UserResponse;
import com.university.exam_lecture_timetable.entity.RoleEntity;
import com.university.exam_lecture_timetable.entity.RoleName;
import com.university.exam_lecture_timetable.entity.UserEntity;
import com.university.exam_lecture_timetable.exception.AccountStatusException;
import com.university.exam_lecture_timetable.exception.ResourceNotFoundException;
import com.university.exam_lecture_timetable.exception.UserAlreadyExistsException;
import com.university.exam_lecture_timetable.repository.RoleRepository;
import com.university.exam_lecture_timetable.repository.UserRepository;
import com.university.exam_lecture_timetable.service.EmailService;
import com.university.exam_lecture_timetable.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public UserServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           EmailService emailService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("A user with email '" + request.getEmail() + "' already exists");
        }

        // Determine initial password
        String rawPassword = request.getPassword();
        boolean generatedTemporary = false;
        if (!StringUtils.hasText(rawPassword)) {
            rawPassword = generateTemporaryPassword();
            generatedTemporary = true;
        }

        UserEntity user = new UserEntity();
        user.setFullName(request.getFullName().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEnabled(true);
        user.setAccountNonLocked(true);
        user.setMustChangePassword(request.isMustChangePassword());
        user.setLecturerId(request.getLecturerId());
        user.setStudentId(request.getStudentId());

        // Resolve and validate assigned roles
        Set<RoleEntity> roles = resolveRoles(request.getRoles());
        user.setRoles(roles);

        UserEntity savedUser = userRepository.save(user);

        // Notify user of credentials
        if (generatedTemporary) {
            emailService.sendTemporaryPasswordEmail(savedUser.getEmail(), rawPassword);
        }

        logger.info("Created new user: {} with roles: {}", savedUser.getEmail(), request.getRoles());
        return UserResponse.fromEntity(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(String search, Pageable pageable) {
        return userRepository.searchUsers(search, pageable)
                .map(UserResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return UserResponse.fromEntity(user);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setFullName(request.getFullName().trim());
        user.setLecturerId(request.getLecturerId());
        user.setStudentId(request.getStudentId());

        UserEntity updatedUser = userRepository.save(user);
        logger.info("Updated user profile details for user id: {}", id);
        return UserResponse.fromEntity(updatedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUserStatus(Long id, UpdateUserStatusRequest request, String currentAdminEmail) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Guard against an administrator disabling or locking their own active account
        if (user.getEmail().equalsIgnoreCase(currentAdminEmail)) {
            if ((request.getEnabled() != null && !request.getEnabled()) ||
                    (request.getAccountNonLocked() != null && !request.getAccountNonLocked())) {
                throw new AccountStatusException("Administrators cannot deactivate or lock their own account");
            }
        }

        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }
        if (request.getAccountNonLocked() != null) {
            user.setAccountNonLocked(request.getAccountNonLocked());
        }

        UserEntity updatedUser = userRepository.save(user);
        logger.info("Updated status for user {}: enabled={}, accountNonLocked={}",
                user.getEmail(), user.isEnabled(), user.isAccountNonLocked());
        return UserResponse.fromEntity(updatedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUserRoles(Long id, UpdateUserRolesRequest request, String currentAdminEmail) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        Set<RoleEntity> resolvedRoles = resolveRoles(request.getRoles());

        // Guard against an admin accidentally revoking ADMIN role from themselves
        if (user.getEmail().equalsIgnoreCase(currentAdminEmail)) {
            boolean hasAdmin = resolvedRoles.stream().anyMatch(r -> r.getName() == RoleName.ADMIN);
            if (!hasAdmin) {
                throw new AccountStatusException("Administrators cannot remove the ADMIN role from their own account");
            }
        }

        user.setRoles(resolvedRoles);
        UserEntity updatedUser = userRepository.save(user);
        logger.info("Updated roles for user {}: {}", user.getEmail(), request.getRoles());
        return UserResponse.fromEntity(updatedUser);
    }

    private Set<RoleEntity> resolveRoles(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            throw new IllegalArgumentException("At least one role must be assigned");
        }

        Set<RoleEntity> resolvedRoles = new HashSet<>();
        for (String roleStr : roleNames) {
            String normalized = roleStr.trim().toUpperCase();
            if (normalized.startsWith("ROLE_")) {
                normalized = normalized.substring(5);
            }

            try {
                RoleName roleNameEnum = RoleName.valueOf(normalized);
                RoleEntity role = roleRepository.findByName(roleNameEnum)
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found in database: " + roleNameEnum));
                resolvedRoles.add(role);
            } catch (IllegalArgumentException ex) {
                throw new ResourceNotFoundException("Invalid role name: '" + roleStr +
                        "'. Allowed values: ADMIN, EXAM_TIMETABLE_OFFICER, LECTURER_TIMETABLE_OFFICER, LECTURER, STUDENT");
            }
        }
        return resolvedRoles;
    }

    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%";
        StringBuilder sb = new StringBuilder("Tmp#");
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        return sb.toString();
    }

}
