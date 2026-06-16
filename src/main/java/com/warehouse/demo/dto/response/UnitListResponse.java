package com.warehouse.demo.dto.response;

import java.util.List;

public record UnitListResponse(
        List<UnitResponse> units
) {}
