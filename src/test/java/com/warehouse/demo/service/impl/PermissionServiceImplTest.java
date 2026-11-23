package com.warehouse.demo.service.impl;

import com.warehouse.demo.dto.response.PermissionListResponse;
import com.warehouse.demo.dto.response.PermissionResponse;
import com.warehouse.demo.exception.EntityNotFoundException;
import com.warehouse.demo.mapper.PermissionMapper;
import com.warehouse.demo.model.Permission;
import com.warehouse.demo.model.PermissionCode;
import com.warehouse.demo.repository.PermissionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceImplTest {

    private static final long PERMISSION_ID = 1L;
    private static final long MISSING_PERMISSION_ID = 5L;
    private static final boolean NOT_SYSTEM_DEFINED = false;
    private static final boolean NOT_DELETED = false;
    private static final String PERMISSION_NAME = "Read units";
    private static final PermissionCode PERMISSION_CODE = PermissionCode.UNITS_READ;

    @Mock
    private PermissionMapper permissionMapper;

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private PermissionServiceImpl permissionService;

    @Test
    @DisplayName("Find all returns the mapped list from the repository")
    void givenStoredPermissions_whenFindAll_thenReturnsMappedList() {
        // given
        Permission storedPermission = permission(PERMISSION_ID);
        List<Permission> permissions = List.of(storedPermission);
        PermissionResponse permissionResponse = response(PERMISSION_ID);
        List<PermissionResponse> permissionResponses = List.of(permissionResponse);
        PermissionListResponse response = new PermissionListResponse(permissionResponses);
        when(permissionRepository.findAll()).thenReturn(permissions);
        when(permissionMapper.permissionsToPermissionListResponse(permissions)).thenReturn(response);

        // when
        PermissionListResponse result = permissionService.findAll();

        // then
        assertSame(response, result);
    }

    @Test
    @DisplayName("Find by id returns the mapped permission")
    void givenStoredPermission_whenFindById_thenReturnsMappedResponse() {
        // given
        Permission permission = permission(PERMISSION_ID);
        PermissionResponse response = response(PERMISSION_ID);
        Optional<Permission> found = Optional.of(permission);
        when(permissionRepository.findById(PERMISSION_ID)).thenReturn(found);
        when(permissionMapper.permissionToPermissionResponse(permission)).thenReturn(response);

        // when
        PermissionResponse result = permissionService.findById(PERMISSION_ID);

        // then
        assertSame(response, result);
    }

    @Test
    @DisplayName("Find by a missing id fails with entity not found")
    void givenMissingId_whenFindById_thenEntityNotFound() {
        // given
        Optional<Permission> missing = Optional.empty();
        when(permissionRepository.findById(MISSING_PERMISSION_ID)).thenReturn(missing);

        // when
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> permissionService.findById(MISSING_PERMISSION_ID)
        );

        // then
        String message = exception.getMessage();
        String missingId = Long.toString(MISSING_PERMISSION_ID);
        assertTrue(message.contains(missingId));
    }

    private static Permission permission(Long id) {
        Permission permission = new Permission();
        permission.setId(id);
        permission.setName(PERMISSION_NAME);
        permission.setCode(PERMISSION_CODE);
        permission.setSystemDefined(NOT_SYSTEM_DEFINED);
        permission.setDeleted(NOT_DELETED);
        return permission;
    }

    private static PermissionResponse response(Long id) {
        return new PermissionResponse(id, PERMISSION_NAME, PERMISSION_CODE, NOT_DELETED, NOT_SYSTEM_DEFINED);
    }
}
