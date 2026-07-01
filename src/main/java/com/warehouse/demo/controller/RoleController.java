package com.warehouse.demo.controller;

import com.warehouse.demo.dto.request.CreateRoleRequest;
import com.warehouse.demo.dto.request.UpdateRoleRequest;
import com.warehouse.demo.dto.response.RoleListResponse;
import com.warehouse.demo.dto.response.RoleResponse;
import com.warehouse.demo.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping("/")
    @PreAuthorize("hasAnyAuthority('ROLES_READ', 'ROLES_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<RoleListResponse> findAll() {
        return ResponseEntity.ok(roleService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLES_READ', 'ROLES_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<RoleResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.findById(id));
    }

    @PostMapping("/")
    @PreAuthorize("hasAnyAuthority('ROLES_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<RoleResponse> save(@RequestBody @Valid CreateRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roleService.save(request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLES_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<RoleResponse> update(@PathVariable Long id,
                                               @RequestBody @Valid UpdateRoleRequest request) {
        return ResponseEntity.ok(roleService.update(id, request));
    }

    @PatchMapping("/{id}/mark-deleted")
    @PreAuthorize("hasAnyAuthority('ROLES_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<RoleResponse> markAsDeleted(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.markAsDeleted(id));
    }

    @PatchMapping("/{id}/unmark-deleted")
    @PreAuthorize("hasAnyAuthority('ROLES_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<RoleResponse> unmarkAsDeleted(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.unmarkAsDeleted(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLES_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
