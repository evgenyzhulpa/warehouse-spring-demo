package com.warehouse.demo.controller;

import com.warehouse.demo.dto.request.CreateWarehouseRequest;
import com.warehouse.demo.dto.request.UpdateWarehouseRequest;
import com.warehouse.demo.dto.response.WarehouseListResponse;
import com.warehouse.demo.dto.response.WarehouseResponse;
import com.warehouse.demo.service.WarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping("/")
    @PreAuthorize("hasAnyAuthority('WAREHOUSES_READ', 'WAREHOUSES_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<WarehouseListResponse> findAll() {
        return ResponseEntity.ok(warehouseService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('WAREHOUSES_READ', 'WAREHOUSES_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<WarehouseResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseService.findById(id));
    }

    @PostMapping("/")
    @PreAuthorize("hasAnyAuthority('WAREHOUSES_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<WarehouseResponse> save(@RequestBody @Valid CreateWarehouseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(warehouseService.save(request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('WAREHOUSES_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<WarehouseResponse> update(@PathVariable Long id,
                                                    @RequestBody @Valid UpdateWarehouseRequest request) {
        return ResponseEntity.ok(warehouseService.update(id, request));
    }

    @PatchMapping("/{id}/mark-deleted")
    @PreAuthorize("hasAnyAuthority('WAREHOUSES_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<WarehouseResponse> markAsDeleted(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseService.markAsDeleted(id));
    }

    @PatchMapping("/{id}/unmark-deleted")
    @PreAuthorize("hasAnyAuthority('WAREHOUSES_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<WarehouseResponse> unmarkAsDeleted(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseService.unmarkAsDeleted(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('WAREHOUSES_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        warehouseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
