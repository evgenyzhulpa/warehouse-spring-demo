package com.warehouse.demo.dto.response;

public record UnitResponse(
        Long id,
        String name,
        String code,
        String description,
        Boolean deleted
) {}
