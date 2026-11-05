package com.university.exam_lecture_timetable.controller;

import com.university.exam_lecture_timetable.dto.request.CreateUserRequest;
import com.university.exam_lecture_timetable.dto.request.UpdateUserRequest;
import com.university.exam_lecture_timetable.dto.request.UpdateUserRolesRequest;
import com.university.exam_lecture_timetable.dto.request.UpdateUserStatusRequest;
import com.university.exam_lecture_timetable.dto.response.ApiResponse;
import com.university.exam_lecture_timetable.dto.response.UserResponse;
import com.university.exam_lecture_timetable.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Create a new user account (Admin only)
     * POST /api/users
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User account created successfully", response));
    }

    /**
     * View list of users with pagination and search filter (Admin only)
     * GET /api/users?search=john&page=0&size=10
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        Sort sort = sortDirection.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<UserResponse> users = userService.getAllUsers(search, pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    /**
     * View a specific user by ID (Admin or own profile)
     * GET /api/users/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.id == #id")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Update permitted details of a user (Admin only)
     * PUT /api/users/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        UserResponse response = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", response));
    }

    /**
     * Activate or deactivate a user account (Admin only)
     * PATCH /api/users/{id}/status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable Long id,
            @RequestBody UpdateUserStatusRequest request,
            org.springframework.security.core.Authentication authentication) {
        String adminEmail = authentication != null ? authentication.getName() : "";
        UserResponse response = userService.updateUserStatus(id, request, adminEmail);
        return ResponseEntity.ok(ApiResponse.success("User status updated successfully", response));
    }

    /**
     * Assign or update roles for a user (Admin only)
     * PATCH /api/users/{id}/roles
     */
    @PatchMapping("/{id}/roles")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRoles(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRolesRequest request,
            org.springframework.security.core.Authentication authentication) {
        String adminEmail = authentication != null ? authentication.getName() : "";
        UserResponse response = userService.updateUserRoles(id, request, adminEmail);
        return ResponseEntity.ok(ApiResponse.success("User roles updated successfully", response));
    }
}
