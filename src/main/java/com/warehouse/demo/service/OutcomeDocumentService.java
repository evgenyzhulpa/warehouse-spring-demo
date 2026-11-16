package com.warehouse.demo.service;

import com.warehouse.demo.dto.request.CreateOutcomeDocumentRequest;
import com.warehouse.demo.dto.request.OutcomeDocumentLineRequest;
import com.warehouse.demo.dto.request.UpdateOutcomeDocumentRequest;
import com.warehouse.demo.dto.response.OutcomeDocumentLineResponse;
import com.warehouse.demo.dto.response.OutcomeDocumentListResponse;
import com.warehouse.demo.dto.response.OutcomeDocumentResponse;
import org.springframework.data.domain.Pageable;

public interface OutcomeDocumentService {
    OutcomeDocumentListResponse findAll(Pageable pageable);
    OutcomeDocumentResponse findById(Long id);
    OutcomeDocumentResponse save(CreateOutcomeDocumentRequest request);
    OutcomeDocumentResponse update(Long id, UpdateOutcomeDocumentRequest request);
    OutcomeDocumentResponse markAsDeleted(Long id);
    OutcomeDocumentResponse unmarkAsDeleted(Long id);
    void delete(Long id);
    OutcomeDocumentLineResponse addLine(Long documentId, OutcomeDocumentLineRequest request);
    OutcomeDocumentLineResponse updateLine(Long documentId, Long lineId, OutcomeDocumentLineRequest request);
    void deleteLine(Long documentId, Long lineId);
}
