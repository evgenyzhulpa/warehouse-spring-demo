package com.warehouse.demo.service;

import com.warehouse.demo.dto.request.CreateRoleRequest;
import com.warehouse.demo.dto.request.UpdateRoleRequest;
import com.warehouse.demo.dto.response.RoleListResponse;
import com.warehouse.demo.dto.response.RoleResponse;

public interface RoleService {
    RoleListResponse findAll();
    RoleResponse findById(Long id);
    RoleResponse save(CreateRoleRequest createRoleRequest);
    RoleResponse update(Long id, UpdateRoleRequest updateRoleRequest);
    RoleResponse markAsDeleted(Long id);
    RoleResponse unmarkAsDeleted(Long id);
    void delete(Long id);
}
