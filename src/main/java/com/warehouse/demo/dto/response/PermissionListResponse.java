package com.warehouse.demo.dto.response;

import java.util.List;

public record PermissionListResponse(
        List<PermissionResponse> permissions
) {
}
