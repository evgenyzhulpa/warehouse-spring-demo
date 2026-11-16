package com.warehouse.demo.service;

import com.warehouse.demo.dto.request.CreateIncomeDocumentRequest;
import com.warehouse.demo.dto.request.IncomeDocumentLineRequest;
import com.warehouse.demo.dto.request.UpdateIncomeDocumentRequest;
import com.warehouse.demo.dto.response.IncomeDocumentLineResponse;
import com.warehouse.demo.dto.response.IncomeDocumentListResponse;
import com.warehouse.demo.dto.response.IncomeDocumentResponse;
import org.springframework.data.domain.Pageable;

public interface IncomeDocumentService {
    IncomeDocumentListResponse findAll(Pageable pageable);
    IncomeDocumentResponse findById(Long id);
    IncomeDocumentResponse save(CreateIncomeDocumentRequest request);
    IncomeDocumentResponse update(Long id, UpdateIncomeDocumentRequest request);
    IncomeDocumentResponse markAsDeleted(Long id);
    IncomeDocumentResponse unmarkAsDeleted(Long id);
    void delete(Long id);
    IncomeDocumentLineResponse addLine(Long documentId, IncomeDocumentLineRequest request);
    IncomeDocumentLineResponse updateLine(Long documentId, Long lineId, IncomeDocumentLineRequest request);
    void deleteLine(Long documentId, Long lineId);
}
