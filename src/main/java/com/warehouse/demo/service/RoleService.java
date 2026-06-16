package com.warehouse.demo.service;

import com.warehouse.demo.dto.request.UpsertRoleRequest;
import com.warehouse.demo.dto.response.RoleListResponse;
import com.warehouse.demo.dto.response.RoleResponse;

public interface RoleService {
    RoleListResponse findAll();
    RoleResponse findById(Long id);
    RoleResponse save(UpsertRoleRequest upsertRoleRequest);
    RoleResponse update(Long id, UpsertRoleRequest upsertRoleRequest);
    void delete(Long id);
}
