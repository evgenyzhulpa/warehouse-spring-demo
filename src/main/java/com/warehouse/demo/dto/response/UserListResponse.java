package com.warehouse.demo.dto.response;

import java.util.List;

public record UserListResponse(
        List<UserSummaryResponse> users
) {
}
