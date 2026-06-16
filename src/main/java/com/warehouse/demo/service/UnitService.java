package com.warehouse.demo.service;

import com.warehouse.demo.dto.request.UpsertUnitRequest;
import com.warehouse.demo.dto.response.UnitListResponse;
import com.warehouse.demo.dto.response.UnitResponse;

public interface UnitService {
    UnitListResponse findAll();
    UnitResponse findById(Long id);
    UnitResponse save(UpsertUnitRequest upsertUnitRequest);
    UnitResponse update(Long id, UpsertUnitRequest upsertUnitRequest);
    void delete(Long id);
}
