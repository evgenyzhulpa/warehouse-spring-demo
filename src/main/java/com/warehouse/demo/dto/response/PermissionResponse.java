package com.warehouse.demo.dto.response;

import com.warehouse.demo.model.PermissionCode;

public record PermissionResponse(
        Long id,
        String name,
        PermissionCode code,
        Boolean deleted,
        Boolean systemDefined) {
}
