package com.warehouse.demo.controller;

import com.warehouse.demo.dto.request.CreateOutcomeDocumentRequest;
import com.warehouse.demo.dto.request.OutcomeDocumentLineRequest;
import com.warehouse.demo.dto.request.UpdateOutcomeDocumentRequest;
import com.warehouse.demo.dto.response.OutcomeDocumentLineResponse;
import com.warehouse.demo.dto.response.OutcomeDocumentListResponse;
import com.warehouse.demo.dto.response.OutcomeDocumentResponse;
import com.warehouse.demo.service.OutcomeDocumentService;
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
@RequestMapping("/outcome-documents")
@RequiredArgsConstructor
public class OutcomeDocumentController {

    private final OutcomeDocumentService outcomeDocumentService;

    @GetMapping("/")
    @PreAuthorize("hasAnyAuthority('OUTCOME_DOCUMENTS_READ', 'OUTCOME_DOCUMENTS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<OutcomeDocumentListResponse> findAll(
            @PageableDefault(size = 20, sort = {"documentDate", "id"}, direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(outcomeDocumentService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('OUTCOME_DOCUMENTS_READ', 'OUTCOME_DOCUMENTS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<OutcomeDocumentResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(outcomeDocumentService.findById(id));
    }

    @PostMapping("/")
    @PreAuthorize("hasAnyAuthority('OUTCOME_DOCUMENTS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<OutcomeDocumentResponse> save(@RequestBody @Valid CreateOutcomeDocumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(outcomeDocumentService.save(request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('OUTCOME_DOCUMENTS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<OutcomeDocumentResponse> update(@PathVariable Long id,
                                                          @RequestBody @Valid UpdateOutcomeDocumentRequest request) {
        return ResponseEntity.ok(outcomeDocumentService.update(id, request));
    }

    @PatchMapping("/{id}/mark-deleted")
    @PreAuthorize("hasAnyAuthority('OUTCOME_DOCUMENTS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<OutcomeDocumentResponse> markAsDeleted(@PathVariable Long id) {
        return ResponseEntity.ok(outcomeDocumentService.markAsDeleted(id));
    }

    @PatchMapping("/{id}/unmark-deleted")
    @PreAuthorize("hasAnyAuthority('OUTCOME_DOCUMENTS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<OutcomeDocumentResponse> unmarkAsDeleted(@PathVariable Long id) {
        return ResponseEntity.ok(outcomeDocumentService.unmarkAsDeleted(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('OUTCOME_DOCUMENTS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        outcomeDocumentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/lines")
    @PreAuthorize("hasAnyAuthority('OUTCOME_DOCUMENTS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<OutcomeDocumentLineResponse> addLine(@PathVariable Long id,
                                                               @RequestBody @Valid OutcomeDocumentLineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(outcomeDocumentService.addLine(id, request));
    }

    @PatchMapping("/{id}/lines/{lineId}")
    @PreAuthorize("hasAnyAuthority('OUTCOME_DOCUMENTS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<OutcomeDocumentLineResponse> updateLine(@PathVariable Long id,
                                                                  @PathVariable Long lineId,
                                                                  @RequestBody @Valid OutcomeDocumentLineRequest request) {
        return ResponseEntity.ok(outcomeDocumentService.updateLine(id, lineId, request));
    }

    @DeleteMapping("/{id}/lines/{lineId}")
    @PreAuthorize("hasAnyAuthority('OUTCOME_DOCUMENTS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<Void> deleteLine(@PathVariable Long id, @PathVariable Long lineId) {
        outcomeDocumentService.deleteLine(id, lineId);
        return ResponseEntity.noContent().build();
    }
}
