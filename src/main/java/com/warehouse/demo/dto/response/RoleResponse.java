package com.warehouse.demo.dto.response;

import java.util.List;

public record RoleResponse(
        Long id,
        String name,
        Boolean deleted,
        Boolean systemDefined,
        List<PermissionResponse> permissions
) {
}
