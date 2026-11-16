package com.warehouse.demo.controller;

import com.warehouse.demo.dto.request.CreateIncomeDocumentRequest;
import com.warehouse.demo.dto.request.IncomeDocumentLineRequest;
import com.warehouse.demo.dto.request.UpdateIncomeDocumentRequest;
import com.warehouse.demo.dto.response.IncomeDocumentLineResponse;
import com.warehouse.demo.dto.response.IncomeDocumentListResponse;
import com.warehouse.demo.dto.response.IncomeDocumentResponse;
import com.warehouse.demo.service.IncomeDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/income-documents")
@RequiredArgsConstructor
public class IncomeDocumentController {

    private final IncomeDocumentService incomeDocumentService;

    @GetMapping("/")
    @PreAuthorize("hasAnyAuthority('INCOME_DOCUMENTS_READ', 'INCOME_DOCUMENTS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<IncomeDocumentListResponse> findAll(
            @PageableDefault(size = 20, sort = {"documentDate", "id"}, direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(incomeDocumentService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('INCOME_DOCUMENTS_READ', 'INCOME_DOCUMENTS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<IncomeDocumentResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(incomeDocumentService.findById(id));
    }

    @PostMapping("/")
    @PreAuthorize("hasAnyAuthority('INCOME_DOCUMENTS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<IncomeDocumentResponse> save(@RequestBody @Valid CreateIncomeDocumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(incomeDocumentService.save(request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('INCOME_DOCUMENTS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<IncomeDocumentResponse> update(@PathVariable Long id,
                                                         @RequestBody @Valid UpdateIncomeDocumentRequest request) {
        return ResponseEntity.ok(incomeDocumentService.update(id, request));
    }

    @PatchMapping("/{id}/mark-deleted")
    @PreAuthorize("hasAnyAuthority('INCOME_DOCUMENTS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<IncomeDocumentResponse> markAsDeleted(@PathVariable Long id) {
        return ResponseEntity.ok(incomeDocumentService.markAsDeleted(id));
    }

    @PatchMapping("/{id}/unmark-deleted")
    @PreAuthorize("hasAnyAuthority('INCOME_DOCUMENTS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<IncomeDocumentResponse> unmarkAsDeleted(@PathVariable Long id) {
        return ResponseEntity.ok(incomeDocumentService.unmarkAsDeleted(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('INCOME_DOCUMENTS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        incomeDocumentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/lines")
    @PreAuthorize("hasAnyAuthority('INCOME_DOCUMENTS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<IncomeDocumentLineResponse> addLine(@PathVariable Long id,
                                                              @RequestBody @Valid IncomeDocumentLineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(incomeDocumentService.addLine(id, request));
    }

    @PatchMapping("/{id}/lines/{lineId}")
    @PreAuthorize("hasAnyAuthority('INCOME_DOCUMENTS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<IncomeDocumentLineResponse> updateLine(@PathVariable Long id,
                                                                 @PathVariable Long lineId,
                                                                 @RequestBody @Valid IncomeDocumentLineRequest request) {
        return ResponseEntity.ok(incomeDocumentService.updateLine(id, lineId, request));
    }

    @DeleteMapping("/{id}/lines/{lineId}")
    @PreAuthorize("hasAnyAuthority('INCOME_DOCUMENTS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<Void> deleteLine(@PathVariable Long id, @PathVariable Long lineId) {
        incomeDocumentService.deleteLine(id, lineId);
        return ResponseEntity.noContent().build();
    }
}
