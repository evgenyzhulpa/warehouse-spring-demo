package com.warehouse.demo.dto.response;

public record WarehouseResponse(
        Long id,
        String name,
        String address,
        Boolean deleted,
        Boolean systemDefined
) {}
