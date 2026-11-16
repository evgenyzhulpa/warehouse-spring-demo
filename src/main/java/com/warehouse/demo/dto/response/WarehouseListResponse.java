package com.warehouse.demo.dto.response;

import java.util.List;

public record WarehouseListResponse(
        List<WarehouseResponse> warehouses
) {}
