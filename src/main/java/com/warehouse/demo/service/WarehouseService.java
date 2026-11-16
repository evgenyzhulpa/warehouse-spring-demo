package com.warehouse.demo.service;

import com.warehouse.demo.dto.request.CreateWarehouseRequest;
import com.warehouse.demo.dto.request.UpdateWarehouseRequest;
import com.warehouse.demo.dto.response.WarehouseListResponse;
import com.warehouse.demo.dto.response.WarehouseResponse;

public interface WarehouseService {
    WarehouseListResponse findAll();
    WarehouseResponse findById(Long id);
    WarehouseResponse save(CreateWarehouseRequest createWarehouseRequest);
    WarehouseResponse update(Long id, UpdateWarehouseRequest updateWarehouseRequest);
    WarehouseResponse markAsDeleted(Long id);
    WarehouseResponse unmarkAsDeleted(Long id);
    void delete(Long id);
}
