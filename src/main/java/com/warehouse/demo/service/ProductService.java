package com.warehouse.demo.service;

import com.warehouse.demo.dto.request.CreateProductRequest;
import com.warehouse.demo.dto.request.UpdateProductRequest;
import com.warehouse.demo.dto.response.ProductListResponse;
import com.warehouse.demo.dto.response.ProductResponse;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    ProductListResponse findAll(String name, String article, Pageable pageable);
    ProductResponse findById(Long id);
    ProductResponse save(CreateProductRequest createProductRequest);
    ProductResponse update(Long id, UpdateProductRequest updateProductRequest);
    ProductResponse markAsDeleted(Long id);
    ProductResponse unmarkAsDeleted(Long id);
    void delete(Long id);
}
