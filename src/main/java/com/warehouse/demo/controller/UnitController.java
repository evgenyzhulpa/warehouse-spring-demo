package com.warehouse.demo.controller;

import com.warehouse.demo.dto.request.CreateUnitRequest;
import com.warehouse.demo.dto.request.UpdateUnitRequest;
import com.warehouse.demo.dto.response.UnitListResponse;
import com.warehouse.demo.dto.response.UnitResponse;
import com.warehouse.demo.service.UnitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/units")
@RequiredArgsConstructor
public class UnitController {

    private final UnitService unitService;

    @GetMapping("/")
    @PreAuthorize("hasAnyAuthority('UNITS_READ', 'UNITS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<UnitListResponse> findAll() {
        return ResponseEntity.ok(unitService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('UNITS_READ', 'UNITS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<UnitResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(unitService.findById(id));
    }

    @PostMapping("/")
    @PreAuthorize("hasAnyAuthority('UNITS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<UnitResponse> save(@RequestBody @Valid CreateUnitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(unitService.save(request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('UNITS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<UnitResponse> update(@PathVariable Long id,
                                               @RequestBody @Valid UpdateUnitRequest request) {
        return ResponseEntity.ok(unitService.update(id, request));
    }

    @PatchMapping("/{id}/mark-deleted")
    @PreAuthorize("hasAnyAuthority('UNITS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<UnitResponse> markAsDeleted(@PathVariable Long id) {
        return ResponseEntity.ok(unitService.markAsDeleted(id));
    }

    @PatchMapping("/{id}/unmark-deleted")
    @PreAuthorize("hasAnyAuthority('UNITS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<UnitResponse> unmarkAsDeleted(@PathVariable Long id) {
        return ResponseEntity.ok(unitService.unmarkAsDeleted(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('UNITS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        unitService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
