package com.warehouse.demo.dto.response;

import java.time.LocalDateTime;

public record IncomeDocumentJournalResponse(
        Long id,
        String number,
        LocalDateTime documentDate,
        Long warehouseId,
        String warehouseName,
        Boolean deleted
) {}
