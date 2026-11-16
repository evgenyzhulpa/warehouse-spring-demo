package com.warehouse.demo.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record OutcomeDocumentResponse(
        Long id,
        String number,
        LocalDateTime documentDate,
        Long warehouseId,
        String warehouseName,
        Boolean deleted,
        List<OutcomeDocumentLineResponse> lines
) {}
