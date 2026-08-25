package com.warehouse.demo.service;

import com.warehouse.demo.dto.request.CreateUserRequest;
import com.warehouse.demo.dto.request.UpdateUserPasswordRequest;
import com.warehouse.demo.dto.request.UpdateUserRequest;
import com.warehouse.demo.dto.response.CurrentUserResponse;
import com.warehouse.demo.dto.response.UserListResponse;
import com.warehouse.demo.dto.response.UserResponse;
import com.warehouse.demo.security.AppPrincipal;

public interface UserService {
    UserListResponse findAll();
    CurrentUserResponse getCurrentUserData(AppPrincipal appPrincipal);
    UserResponse findById(Long id);
    UserResponse save(CreateUserRequest createUserRequest);
    UserResponse update(Long id, UpdateUserRequest updateUserRequest);
    UserResponse changePassword(Long id, UpdateUserPasswordRequest updateUserPasswordRequest);
    UserResponse markAsDeleted(Long id);
    UserResponse unmarkAsDeleted(Long id);
    void delete(Long id);
}
