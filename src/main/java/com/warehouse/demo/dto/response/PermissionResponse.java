package com.warehouse.demo.dto.response;

import com.warehouse.demo.model.PermissionCode;

public record PermissionResponse(
        String name,
        PermissionCode code) {
}
