package com.warehouse.demo.controller;

import com.warehouse.demo.dto.response.PermissionListResponse;
import com.warehouse.demo.dto.response.PermissionResponse;
import com.warehouse.demo.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping("/")
    @PreAuthorize("hasAnyAuthority('ROLES_READ', 'ROLES_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<PermissionListResponse> findAll() {
        return ResponseEntity.ok(permissionService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLES_READ', 'ROLES_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<PermissionResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(permissionService.findById(id));
    }
}
