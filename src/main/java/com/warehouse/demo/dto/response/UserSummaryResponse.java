package com.warehouse.demo.dto.response;

import java.util.List;

public record UserSummaryResponse(
        Long id,
        String username,
        Boolean enabled,
        Boolean deleted,
        Boolean systemDefined,
        String lastName,
        String firstName,
        String middleName
) {
}
