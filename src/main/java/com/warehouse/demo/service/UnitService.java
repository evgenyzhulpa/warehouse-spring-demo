package com.warehouse.demo.service;

import com.warehouse.demo.dto.request.CreateUnitRequest;
import com.warehouse.demo.dto.request.UpdateUnitRequest;
import com.warehouse.demo.dto.response.UnitListResponse;
import com.warehouse.demo.dto.response.UnitResponse;

public interface UnitService {
    UnitListResponse findAll();
    UnitResponse findById(Long id);
    UnitResponse save(CreateUnitRequest createUnitRequest);
    UnitResponse update(Long id, UpdateUnitRequest updateUnitRequest);
    UnitResponse markAsDeleted(Long id);
    UnitResponse unmarkAsDeleted(Long id);
    void delete(Long id);
}
