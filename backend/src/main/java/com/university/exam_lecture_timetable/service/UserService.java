package com.university.exam_lecture_timetable.service;

import com.university.exam_lecture_timetable.dto.request.CreateUserRequest;
import com.university.exam_lecture_timetable.dto.request.UpdateUserRequest;
import com.university.exam_lecture_timetable.dto.request.UpdateUserRolesRequest;
import com.university.exam_lecture_timetable.dto.request.UpdateUserStatusRequest;
import com.university.exam_lecture_timetable.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserResponse createUser(CreateUserRequest request);

    Page<UserResponse> getAllUsers(String search, Pageable pageable);

    UserResponse getUserById(Long id);

    UserResponse updateUser(Long id, UpdateUserRequest request);

    UserResponse updateUserStatus(Long id, UpdateUserStatusRequest request, String currentAdminEmail);

    UserResponse updateUserRoles(Long id, UpdateUserRolesRequest request, String currentAdminEmail);
}
