package com.warehouse.demo.controller;

import com.warehouse.demo.dto.request.UpsertUnitRequest;
import com.warehouse.demo.dto.response.UnitListResponse;
import com.warehouse.demo.dto.response.UnitResponse;
import com.warehouse.demo.service.UnitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/units")
@RequiredArgsConstructor
public class UnitController {

    private final UnitService unitService;

    @GetMapping("/")
    public ResponseEntity<UnitListResponse> findAll() {
        return ResponseEntity.ok(unitService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UnitResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(unitService.findById(id));
    }

    @PostMapping("/")
    public ResponseEntity<UnitResponse> save(@RequestBody @Valid UpsertUnitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(unitService.save(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UnitResponse> update(@PathVariable Long id,
                                               @RequestBody @Valid UpsertUnitRequest request) {
        return ResponseEntity.ok(unitService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        unitService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
