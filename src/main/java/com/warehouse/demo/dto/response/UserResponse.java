package com.warehouse.demo.dto.response;

import java.util.List;

public record UserResponse(
        Long id,
        String username,
        Boolean enabled,
        String lastName,
        String firstName,
        String middleName,
        Boolean deleted,
        Boolean systemDefined,
        List<RoleResponse> roles
) {
}
