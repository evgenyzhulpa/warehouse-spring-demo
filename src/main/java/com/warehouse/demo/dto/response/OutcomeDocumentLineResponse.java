package com.warehouse.demo.dto.response;

import java.math.BigDecimal;

public record OutcomeDocumentLineResponse(
        Long id,
        Long productId,
        String productName,
        String productArticle,
        BigDecimal quantity
) {}
