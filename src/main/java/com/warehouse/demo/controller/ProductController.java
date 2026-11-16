package com.warehouse.demo.controller;

import com.warehouse.demo.dto.request.CreateProductRequest;
import com.warehouse.demo.dto.request.UpdateProductRequest;
import com.warehouse.demo.dto.response.ProductListResponse;
import com.warehouse.demo.dto.response.ProductResponse;
import com.warehouse.demo.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/")
    @PreAuthorize("hasAnyAuthority('PRODUCTS_READ', 'PRODUCTS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<ProductListResponse> findAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String article,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(productService.findAll(name, article, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PRODUCTS_READ', 'PRODUCTS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<ProductResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    @PostMapping("/")
    @PreAuthorize("hasAnyAuthority('PRODUCTS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<ProductResponse> save(@RequestBody @Valid CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.save(request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PRODUCTS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<ProductResponse> update(@PathVariable Long id,
                                                  @RequestBody @Valid UpdateProductRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @PatchMapping("/{id}/mark-deleted")
    @PreAuthorize("hasAnyAuthority('PRODUCTS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<ProductResponse> markAsDeleted(@PathVariable Long id) {
        return ResponseEntity.ok(productService.markAsDeleted(id));
    }

    @PatchMapping("/{id}/unmark-deleted")
    @PreAuthorize("hasAnyAuthority('PRODUCTS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<ProductResponse> unmarkAsDeleted(@PathVariable Long id) {
        return ResponseEntity.ok(productService.unmarkAsDeleted(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('PRODUCTS_WRITE', 'FULL_ACCESS')")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
