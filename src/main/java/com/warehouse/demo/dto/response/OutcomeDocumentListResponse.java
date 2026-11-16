package com.warehouse.demo.dto.response;

import java.util.List;

public record OutcomeDocumentListResponse(
        List<OutcomeDocumentJournalResponse> outcomeDocuments,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
