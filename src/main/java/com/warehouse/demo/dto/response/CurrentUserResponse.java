package com.warehouse.demo.dto.response;

import com.warehouse.demo.model.PermissionCode;

import java.util.Set;

public record CurrentUserResponse(
        Long id,
        String username,
        Set<PermissionCode> permissionCodes
) {
}
