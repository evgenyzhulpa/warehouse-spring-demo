package com.warehouse.demo.dto.response;

import java.util.List;

public record ProductListResponse(
        List<ProductResponse> products,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
