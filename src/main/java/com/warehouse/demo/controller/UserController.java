package com.warehouse.demo.controller;

import com.warehouse.demo.dto.request.CreateUserRequest;
import com.warehouse.demo.dto.request.UpdateUserPasswordRequest;
import com.warehouse.demo.dto.request.UpdateUserRequest;
import com.warehouse.demo.dto.response.CurrentUserResponse;
import com.warehouse.demo.dto.response.UnitResponse;
import com.warehouse.demo.dto.response.UserListResponse;
import com.warehouse.demo.dto.response.UserResponse;
import com.warehouse.demo.security.AppPrincipal;
import com.warehouse.demo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/")
    @PreAuthorize("hasAnyAuthority('USERS_READ', 'USERS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<UserListResponse> findAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> getCurrentUserData(
            @AuthenticationPrincipal AppPrincipal appPrincipal) {
        return ResponseEntity.ok(userService.getCurrentUserData(appPrincipal));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('USERS_READ', 'USERS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<UserResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PostMapping("/")
    @PreAuthorize("hasAnyAuthority('USERS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<UserResponse> save(@RequestBody @Valid CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.save(request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('USERS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<UserResponse> update(@PathVariable Long id,
                                               @RequestBody @Valid UpdateUserRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @PatchMapping("/{id}/change-password")
    @PreAuthorize("hasAnyAuthority('USERS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<UserResponse> changePassword(@PathVariable Long id,
                                                       @RequestBody @Valid UpdateUserPasswordRequest request) {
        return ResponseEntity.ok(userService.changePassword(id, request));
    }

    @PatchMapping("/{id}/mark-deleted")
    @PreAuthorize("hasAnyAuthority('USERS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<UserResponse> markAsDeleted(@PathVariable Long id) {
        return ResponseEntity.ok(userService.markAsDeleted(id));
    }

    @PatchMapping("/{id}/unmark-deleted")
    @PreAuthorize("hasAnyAuthority('USERS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<UserResponse> unmarkAsDeleted(@PathVariable Long id) {
        return ResponseEntity.ok(userService.unmarkAsDeleted(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('USERS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
