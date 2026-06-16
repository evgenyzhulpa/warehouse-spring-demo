package com.warehouse.demo.service.impl;

import com.warehouse.demo.dto.response.PermissionListResponse;
import com.warehouse.demo.dto.response.PermissionResponse;
import com.warehouse.demo.exception.EntityNotFoundException;
import com.warehouse.demo.mapper.PermissionMapper;
import com.warehouse.demo.model.Permission;
import com.warehouse.demo.repository.PermissionRepository;
import com.warehouse.demo.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionMapper permissionMapper;
    private final PermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    @Override
    public PermissionListResponse findAll() {
        List<Permission> permissions = permissionRepository.findAll();
        return permissionMapper.permissionsToPermissionListResponse(permissions);
    }

    @Transactional(readOnly = true)
    @Override
    public PermissionResponse findById(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        MessageFormat.format("Право с ID {0} не найдено", id)
                ));
        return permissionMapper.permissionToPermissionResponse(permission);
    }
}
