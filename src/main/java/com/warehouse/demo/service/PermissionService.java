package com.warehouse.demo.service;

import com.warehouse.demo.dto.response.PermissionListResponse;
import com.warehouse.demo.dto.response.PermissionResponse;

public interface PermissionService {
    PermissionListResponse findAll();
    PermissionResponse findById(Long id);
}
