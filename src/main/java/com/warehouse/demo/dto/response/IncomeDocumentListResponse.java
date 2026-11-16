package com.warehouse.demo.dto.response;

import java.util.List;

public record IncomeDocumentListResponse(
        List<IncomeDocumentJournalResponse> incomeDocuments,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
