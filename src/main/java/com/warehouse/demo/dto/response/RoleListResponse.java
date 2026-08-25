package com.warehouse.demo.dto.response;

import java.util.List;

public record RoleListResponse(
        List<RoleSummaryResponse> roles
) {
}
