package com.warehouse.demo.service.impl;

import com.warehouse.demo.dto.request.CreateRoleRequest;
import com.warehouse.demo.dto.request.UpdateRoleRequest;
import com.warehouse.demo.dto.response.RoleListResponse;
import com.warehouse.demo.dto.response.RoleResponse;
import com.warehouse.demo.exception.BusinessLogicException;
import com.warehouse.demo.exception.EntityNotFoundException;
import com.warehouse.demo.mapper.RoleMapper;
import com.warehouse.demo.model.Permission;
import com.warehouse.demo.model.Role;
import com.warehouse.demo.repository.PermissionRepository;
import com.warehouse.demo.repository.RoleRepository;
import com.warehouse.demo.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    @Override
    public RoleListResponse findAll() {
        List<Role> roles = roleRepository.findAll();
        return roleMapper.rolesToRoleListResponse(roles);
    }

    @Transactional(readOnly = true)
    @Override
    public RoleResponse findById(Long id) {
        return roleMapper.roleToRoleResponse(findRoleById(id));
    }

    private Role findRoleById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        MessageFormat.format("Роль с id {0} не найдена", id)
                ));
    }

    @Transactional
    @Override
    public RoleResponse save(CreateRoleRequest createRoleRequest) {
        Role newRole = roleMapper.createRoleRequestToRole(createRoleRequest);
        Set<Permission> permissions = findPermissionsById(
                createRoleRequest.permissionsIdentifiers());
        newRole.setPermissions(permissions);
        return roleMapper.roleToRoleResponse(roleRepository.save(newRole));
    }

    private Set<Permission> findPermissionsById(List<Long> permissionsIdentifiers) {

        Set<Permission> permissions = new HashSet<>(
                permissionRepository.findAllById(permissionsIdentifiers));
        if (permissionsIdentifiers.size() == permissions.size()) {
            return permissions;
        }

        List<Long> foundIds = permissions
                .stream()
                .map(Permission::getId)
                .toList();

        List<Long> missingIds = permissionsIdentifiers
                .stream()
                .filter(id -> !foundIds.contains(id))
                .toList();

        throw new EntityNotFoundException(
                MessageFormat.format("Не удалось найти права c id: {0}", missingIds));
    }

    @Transactional
    @Override
    public RoleResponse update(Long id, UpdateRoleRequest updateRoleRequest) {
        Role existingRole = findRoleById(id);
        if (existingRole.getSystemDefined()) {
            throw new BusinessLogicException("Редактирование предопределенных ролей запрещено");
        }

        roleMapper.updateRoleFromUpdateRoleRequest(updateRoleRequest, existingRole);

        List<Long> permissionsIdentifiers = updateRoleRequest.permissionsIdentifiers();
        if (permissionsIdentifiers != null) {
            Set<Permission> permissions = findPermissionsById(permissionsIdentifiers);
            existingRole.setPermissions(permissions);
        }

        return roleMapper.roleToRoleResponse(roleRepository.save(existingRole));
    }

    @Transactional
    @Override
    public RoleResponse markAsDeleted(Long id) {
        return setDeletedValue(id, true);
    }

    private RoleResponse setDeletedValue(Long id, boolean isDeleted) {
        Role existingRole = findRoleById(id);
        if (isDeleted && existingRole.getSystemDefined()) {
            throw new BusinessLogicException("Редактирование предопределенных ролей запрещено");
        }
        existingRole.setDeleted(isDeleted);
        return roleMapper.roleToRoleResponse(roleRepository.save(existingRole));
    }

    @Transactional
    @Override
    public RoleResponse unmarkAsDeleted(Long id) {
        return setDeletedValue(id, false);
    }

    @Transactional
    @Override
    public void delete(Long id) {
        Role existingRole = findRoleById(id);
        if (existingRole.getSystemDefined()) {
            throw new BusinessLogicException("Удаление предопределенных ролей запрещено");
        }
        roleRepository.delete(existingRole);
    }
}
