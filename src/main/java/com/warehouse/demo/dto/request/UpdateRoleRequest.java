package com.warehouse.demo.dto.request;

import java.util.List;

public record UpdateRoleRequest(
        String name,
        List<Long> permissionsIdentifiers
) {
}
