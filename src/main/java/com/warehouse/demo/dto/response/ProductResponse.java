package com.warehouse.demo.dto.response;

public record ProductResponse(
        Long id,
        String name,
        String article,
        Long unitId,
        String unitName,
        Boolean deleted,
        Boolean systemDefined
) {}
