package com.warehouse.demo.service.impl;

import com.warehouse.demo.dto.request.CreateRoleRequest;
import com.warehouse.demo.dto.request.UpdateRoleRequest;
import com.warehouse.demo.dto.response.PermissionResponse;
import com.warehouse.demo.dto.response.RoleListResponse;
import com.warehouse.demo.dto.response.RoleResponse;
import com.warehouse.demo.dto.response.RoleSummaryResponse;
import com.warehouse.demo.exception.BusinessLogicException;
import com.warehouse.demo.exception.EntityNotFoundException;
import com.warehouse.demo.mapper.RoleMapper;
import com.warehouse.demo.model.Permission;
import com.warehouse.demo.model.PermissionCode;
import com.warehouse.demo.model.Role;
import com.warehouse.demo.repository.PermissionRepository;
import com.warehouse.demo.repository.RoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    private static final long ROLE_ID = 1L;
    private static final long MISSING_ROLE_ID = 5L;
    private static final long PERMISSION_ID = 3L;
    private static final long MISSING_PERMISSION_ID = 9L;
    private static final boolean SYSTEM_DEFINED = true;
    private static final boolean NOT_SYSTEM_DEFINED = false;
    private static final boolean DELETED = true;
    private static final boolean NOT_DELETED = false;
    private static final String STOREKEEPER_NAME = "Storekeeper";
    private static final String MANAGER_NAME = "Manager";
    private static final String DELETE_FORBIDDEN_MESSAGE = "Удаление предопределенных ролей запрещено";

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private RoleServiceImpl roleService;

    @Test
    @DisplayName("Find all returns the mapped list from the repository")
    void givenStoredRoles_whenFindAll_thenReturnsMappedList() {
        // given
        Role storedRole = role(ROLE_ID, NOT_SYSTEM_DEFINED, NOT_DELETED);
        List<Role> roles = List.of(storedRole);
        RoleSummaryResponse summary = summary(ROLE_ID);
        List<RoleSummaryResponse> summaries = List.of(summary);
        RoleListResponse response = new RoleListResponse(summaries);
        when(roleRepository.findAll()).thenReturn(roles);
        when(roleMapper.rolesToRoleListResponse(roles)).thenReturn(response);

        // when
        RoleListResponse result = roleService.findAll();

        // then
        assertSame(response, result);
    }

    @Test
    @DisplayName("Find by id returns the mapped role")
    void givenStoredRole_whenFindById_thenReturnsMappedResponse() {
        // given
        Role role = role(ROLE_ID, NOT_SYSTEM_DEFINED, NOT_DELETED);
        RoleResponse response = response(ROLE_ID);
        Optional<Role> found = Optional.of(role);
        when(roleRepository.findById(ROLE_ID)).thenReturn(found);
        when(roleMapper.roleToRoleResponse(role)).thenReturn(response);

        // when
        RoleResponse result = roleService.findById(ROLE_ID);

        // then
        assertSame(response, result);
    }

    @Test
    @DisplayName("Find by a missing id fails with entity not found")
    void givenMissingId_whenFindById_thenEntityNotFound() {
        // given
        Optional<Role> missing = Optional.empty();
        when(roleRepository.findById(MISSING_ROLE_ID)).thenReturn(missing);

        // when
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> roleService.findById(MISSING_ROLE_ID)
        );

        // then
        String message = exception.getMessage();
        String missingId = Long.toString(MISSING_ROLE_ID);
        assertTrue(message.contains(missingId));
    }

    @Test
    @DisplayName("Save attaches the permissions and returns the mapped response")
    void givenCreateRequest_whenSave_thenReturnsMappedResponse() {
        // given
        List<Long> permissionIds = List.of(PERMISSION_ID);
        CreateRoleRequest request = new CreateRoleRequest(STOREKEEPER_NAME, permissionIds);
        Role mapped = new Role();
        Permission permission = permission(PERMISSION_ID);
        List<Permission> permissions = List.of(permission);
        RoleResponse response = response(ROLE_ID);
        when(roleMapper.createRoleRequestToRole(request)).thenReturn(mapped);
        when(permissionRepository.findAllById(permissionIds)).thenReturn(permissions);
        when(roleRepository.save(mapped)).thenReturn(mapped);
        when(roleMapper.roleToRoleResponse(mapped)).thenReturn(response);

        // when
        RoleResponse result = roleService.save(request);

        // then
        Set<Permission> assigned = mapped.getPermissions();
        boolean permissionAssigned = assigned.contains(permission);
        assertSame(response, result);
        assertTrue(permissionAssigned);
    }

    @Test
    @DisplayName("Save fails when a permission does not exist and is not stored")
    void givenMissingPermission_whenSave_thenDoesNotSave() {
        // given
        List<Long> permissionIds = List.of(MISSING_PERMISSION_ID);
        CreateRoleRequest request = new CreateRoleRequest(STOREKEEPER_NAME, permissionIds);
        Role mapped = new Role();
        List<Permission> noPermissions = List.of();
        when(roleMapper.createRoleRequestToRole(request)).thenReturn(mapped);
        when(permissionRepository.findAllById(permissionIds)).thenReturn(noPermissions);

        // when
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> roleService.save(request)
        );

        // then
        String message = exception.getMessage();
        String missingId = Long.toString(MISSING_PERMISSION_ID);
        assertTrue(message.contains(missingId));
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    @DisplayName("Update of an ordinary role is saved with the selected permissions")
    void givenOrdinaryRole_whenUpdate_thenSavesMappedRole() {
        // given
        Role role = role(ROLE_ID, NOT_SYSTEM_DEFINED, NOT_DELETED);
        Permission permission = permission(PERMISSION_ID);
        List<Long> permissionIds = List.of(PERMISSION_ID);
        List<Permission> permissions = List.of(permission);
        UpdateRoleRequest request = new UpdateRoleRequest(MANAGER_NAME, permissionIds);
        RoleResponse response = response(ROLE_ID);
        Optional<Role> found = Optional.of(role);
        when(roleRepository.findById(ROLE_ID)).thenReturn(found);
        when(permissionRepository.findAllById(permissionIds)).thenReturn(permissions);
        when(roleRepository.save(role)).thenReturn(role);
        when(roleMapper.roleToRoleResponse(role)).thenReturn(response);

        // when
        RoleResponse result = roleService.update(ROLE_ID, request);

        // then
        Set<Permission> assigned = role.getPermissions();
        boolean permissionAssigned = assigned.contains(permission);
        assertSame(response, result);
        assertTrue(permissionAssigned);
        verify(roleMapper).updateRoleFromUpdateRoleRequest(request, role);
        verify(roleRepository).save(role);
    }

    @Test
    @DisplayName("Update without permissions keeps the role and does not load permissions")
    void givenOrdinaryRoleWithoutPermissions_whenUpdate_thenSavesRole() {
        // given
        Role role = role(ROLE_ID, NOT_SYSTEM_DEFINED, NOT_DELETED);
        UpdateRoleRequest request = new UpdateRoleRequest(MANAGER_NAME, null);
        RoleResponse response = response(ROLE_ID);
        Optional<Role> found = Optional.of(role);
        when(roleRepository.findById(ROLE_ID)).thenReturn(found);
        when(roleRepository.save(role)).thenReturn(role);
        when(roleMapper.roleToRoleResponse(role)).thenReturn(response);

        // when
        RoleResponse result = roleService.update(ROLE_ID, request);

        // then
        assertSame(response, result);
        verify(permissionRepository, never()).findAllById(any());
        verify(roleRepository).save(role);
    }

    @Test
    @DisplayName("Update of a system-defined role is rejected and is not saved")
    void givenSystemDefinedRole_whenUpdate_thenDoesNotSave() {
        // given
        Role role = role(ROLE_ID, SYSTEM_DEFINED, NOT_DELETED);
        List<Long> permissionIds = List.of(PERMISSION_ID);
        UpdateRoleRequest request = new UpdateRoleRequest(STOREKEEPER_NAME, permissionIds);
        Optional<Role> found = Optional.of(role);
        when(roleRepository.findById(ROLE_ID)).thenReturn(found);

        // when
        assertThrows(
                BusinessLogicException.class,
                () -> roleService.update(ROLE_ID, request)
        );

        // then
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    @DisplayName("Update fails when a permission does not exist and is not stored")
    void givenMissingPermission_whenUpdate_thenDoesNotSave() {
        // given
        Role role = role(ROLE_ID, NOT_SYSTEM_DEFINED, NOT_DELETED);
        List<Long> permissionIds = List.of(MISSING_PERMISSION_ID);
        List<Permission> noPermissions = List.of();
        UpdateRoleRequest request = new UpdateRoleRequest(STOREKEEPER_NAME, permissionIds);
        Optional<Role> found = Optional.of(role);
        when(roleRepository.findById(ROLE_ID)).thenReturn(found);
        when(permissionRepository.findAllById(permissionIds)).thenReturn(noPermissions);

        // when
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> roleService.update(ROLE_ID, request)
        );

        // then
        String message = exception.getMessage();
        String missingId = Long.toString(MISSING_PERMISSION_ID);
        assertTrue(message.contains(missingId));
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    @DisplayName("An ordinary role is marked deleted")
    void givenOrdinaryRole_whenMarkDeleted_thenSavesMarkedRole() {
        // given
        Role role = role(ROLE_ID, NOT_SYSTEM_DEFINED, NOT_DELETED);
        RoleResponse response = response(ROLE_ID);
        Optional<Role> found = Optional.of(role);
        when(roleRepository.findById(ROLE_ID)).thenReturn(found);
        when(roleRepository.save(role)).thenReturn(role);
        when(roleMapper.roleToRoleResponse(role)).thenReturn(response);

        // when
        roleService.markAsDeleted(ROLE_ID);

        // then
        Boolean deleted = role.getDeleted();
        assertTrue(Boolean.TRUE.equals(deleted));
        verify(roleRepository).save(role);
    }

    @Test
    @DisplayName("Marking a system-defined role deleted is rejected")
    void givenSystemDefinedRole_whenMarkDeleted_thenDoesNotSave() {
        // given
        Role role = role(ROLE_ID, SYSTEM_DEFINED, NOT_DELETED);
        Optional<Role> found = Optional.of(role);
        when(roleRepository.findById(ROLE_ID)).thenReturn(found);

        // when
        assertThrows(BusinessLogicException.class, () -> roleService.markAsDeleted(ROLE_ID));

        // then
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    @DisplayName("Unmarking an ordinary role clears the deleted flag")
    void givenDeletedOrdinaryRole_whenUnmark_thenClearsDeleted() {
        // given
        Role role = role(ROLE_ID, NOT_SYSTEM_DEFINED, DELETED);
        RoleResponse response = response(ROLE_ID);
        Optional<Role> found = Optional.of(role);
        when(roleRepository.findById(ROLE_ID)).thenReturn(found);
        when(roleRepository.save(role)).thenReturn(role);
        when(roleMapper.roleToRoleResponse(role)).thenReturn(response);

        // when
        roleService.unmarkAsDeleted(ROLE_ID);

        // then
        Boolean deleted = role.getDeleted();
        assertTrue(Boolean.FALSE.equals(deleted));
        verify(roleRepository).save(role);
    }

    @Test
    @DisplayName("Unmarking a system-defined role clears the deleted flag")
    void givenDeletedSystemDefinedRole_whenUnmark_thenClearsDeleted() {
        // given
        Role role = role(ROLE_ID, SYSTEM_DEFINED, DELETED);
        RoleResponse response = response(ROLE_ID);
        Optional<Role> found = Optional.of(role);
        when(roleRepository.findById(ROLE_ID)).thenReturn(found);
        when(roleRepository.save(role)).thenReturn(role);
        when(roleMapper.roleToRoleResponse(role)).thenReturn(response);

        // when
        RoleResponse result = roleService.unmarkAsDeleted(ROLE_ID);

        // then
        Boolean deleted = role.getDeleted();
        assertSame(response, result);
        assertTrue(Boolean.FALSE.equals(deleted));
        verify(roleRepository).save(role);
    }

    @Test
    @DisplayName("An ordinary role is deleted from the repository")
    void givenOrdinaryRole_whenDelete_thenDeletesRole() {
        // given
        Role role = role(ROLE_ID, NOT_SYSTEM_DEFINED, NOT_DELETED);
        Optional<Role> found = Optional.of(role);
        when(roleRepository.findById(ROLE_ID)).thenReturn(found);

        // when
        roleService.delete(ROLE_ID);

        // then
        verify(roleRepository).delete(role);
    }

    @Test
    @DisplayName("Delete of a system-defined role is rejected")
    void givenSystemDefinedRole_whenDelete_thenDoesNotDelete() {
        // given
        Role role = role(ROLE_ID, SYSTEM_DEFINED, NOT_DELETED);
        Optional<Role> found = Optional.of(role);
        when(roleRepository.findById(ROLE_ID)).thenReturn(found);

        // when
        BusinessLogicException exception = assertThrows(
                BusinessLogicException.class,
                () -> roleService.delete(ROLE_ID)
        );

        // then
        String message = exception.getMessage();
        assertTrue(message.contains(DELETE_FORBIDDEN_MESSAGE));
        verify(roleRepository, never()).delete(any(Role.class));
    }

    private static Role role(Long id, boolean systemDefined, boolean deleted) {
        Role role = new Role();
        role.setId(id);
        role.setName(STOREKEEPER_NAME);
        role.setSystemDefined(systemDefined);
        role.setDeleted(deleted);
        return role;
    }

    private static Permission permission(Long id) {
        Permission permission = new Permission();
        permission.setId(id);
        permission.setCode(PermissionCode.ROLES_READ);
        permission.setDeleted(NOT_DELETED);
        permission.setSystemDefined(NOT_SYSTEM_DEFINED);
        return permission;
    }

    private static RoleResponse response(Long id) {
        List<PermissionResponse> permissions = List.of();
        return new RoleResponse(id, STOREKEEPER_NAME, NOT_DELETED, NOT_SYSTEM_DEFINED, permissions);
    }

    private static RoleSummaryResponse summary(Long id) {
        return new RoleSummaryResponse(id, NOT_DELETED, NOT_SYSTEM_DEFINED, STOREKEEPER_NAME);
    }
}
