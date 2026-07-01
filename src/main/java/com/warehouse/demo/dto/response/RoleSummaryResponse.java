package com.warehouse.demo.dto.response;

public record RoleSummaryResponse(
        Long id,
        Boolean deleted,
        Boolean systemDefined,
        String name
) {
}
