package com.warehouse.demo.service.impl;

import com.warehouse.demo.dto.request.UpsertRoleRequest;
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
import com.warehouse.demo.utils.BeanUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.util.List;

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
    public RoleResponse save(UpsertRoleRequest upsertRoleRequest) {
        Role newRole = new Role();
        newRole.setName(upsertRoleRequest.name());
        List<Permission> permissions = permissionRepository.findAllById(
                upsertRoleRequest.permissionsIdentifiers());
        newRole.setPermissions(permissions);
        return roleMapper.roleToRoleResponse(roleRepository.save(newRole));
    }

    @Transactional
    @Override
    public RoleResponse update(Long id, UpsertRoleRequest upsertRoleRequest) {
        Role existingRole = findRoleById(id);
        if (existingRole.getSystemDefined()) {
            throw new BusinessLogicException("Редактирование предопределенных ролей запрещено");
        }
        BeanUtils.copyNonNullProperties(upsertRoleRequest, existingRole);
        return roleMapper.roleToRoleResponse(roleRepository.save(existingRole));
    }

    @Transactional
    @Override
    public void delete(Long id) {
        Role existingRole = findRoleById(id);
        if (existingRole.getSystemDefined()) {
            throw new BusinessLogicException("Удаление предопределенных ролей запрещено");
        }
        existingRole.setDeleted(true);
        roleMapper.roleToRoleResponse(roleRepository.save(existingRole));
    }
}
