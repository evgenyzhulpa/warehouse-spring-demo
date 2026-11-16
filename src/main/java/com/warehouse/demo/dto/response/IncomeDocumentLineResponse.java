package com.warehouse.demo.dto.response;

import java.math.BigDecimal;

public record IncomeDocumentLineResponse(
        Long id,
        Long productId,
        String productName,
        String productArticle,
        BigDecimal quantity
) {}
