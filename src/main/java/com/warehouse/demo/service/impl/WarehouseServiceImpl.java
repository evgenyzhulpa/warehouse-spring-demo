package com.warehouse.demo.service.impl;

import com.warehouse.demo.dto.request.CreateWarehouseRequest;
import com.warehouse.demo.dto.request.UpdateWarehouseRequest;
import com.warehouse.demo.dto.response.WarehouseListResponse;
import com.warehouse.demo.dto.response.WarehouseResponse;
import com.warehouse.demo.exception.BusinessLogicException;
import com.warehouse.demo.exception.EntityNotFoundException;
import com.warehouse.demo.mapper.WarehouseMapper;
import com.warehouse.demo.model.Warehouse;
import com.warehouse.demo.repository.WarehouseRepository;
import com.warehouse.demo.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseMapper warehouseMapper;
    private final WarehouseRepository warehouseRepository;

    @Transactional(readOnly = true)
    @Override
    public WarehouseListResponse findAll() {
        return warehouseMapper.warehouseListToWarehouseListResponse(
                warehouseRepository.findAll());
    }

    @Transactional(readOnly = true)
    @Override
    public WarehouseResponse findById(Long id) {
        return warehouseMapper.warehouseToWarehouseResponse(findWarehouseById(id));
    }

    private Warehouse findWarehouseById(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        MessageFormat.format("Склад с ID {0} не найден!", id)
                ));
    }

    @Transactional
    @Override
    public WarehouseResponse save(CreateWarehouseRequest createWarehouseRequest) {
        Warehouse newWarehouse = warehouseRepository.save(
                warehouseMapper.createWarehouseRequestToWarehouse(createWarehouseRequest));
        return warehouseMapper.warehouseToWarehouseResponse(newWarehouse);
    }

    @Transactional
    @Override
    public WarehouseResponse update(Long id, UpdateWarehouseRequest updateWarehouseRequest) {
        Warehouse existingWarehouse = findWarehouseById(id);
        if (existingWarehouse.getSystemDefined()) {
            throw new BusinessLogicException("Редактирование предопределенных складов запрещено");
        }
        warehouseMapper.updateWarehouseFromUpdateWarehouseRequest(updateWarehouseRequest, existingWarehouse);
        return warehouseMapper.warehouseToWarehouseResponse(warehouseRepository.save(existingWarehouse));
    }

    @Transactional
    @Override
    public WarehouseResponse markAsDeleted(Long id) {
        return setDeletedValue(id, true);
    }

    private WarehouseResponse setDeletedValue(Long id, boolean isDeleted) {
        Warehouse existingWarehouse = findWarehouseById(id);
        if (isDeleted && existingWarehouse.getSystemDefined()) {
            throw new BusinessLogicException("Редактирование предопределенных складов запрещено");
        }
        existingWarehouse.setDeleted(isDeleted);
        return warehouseMapper.warehouseToWarehouseResponse(warehouseRepository.save(existingWarehouse));
    }

    @Transactional
    @Override
    public WarehouseResponse unmarkAsDeleted(Long id) {
        return setDeletedValue(id, false);
    }

    @Transactional
    @Override
    public void delete(Long id) {
        Warehouse existingWarehouse = findWarehouseById(id);
        if (existingWarehouse.getSystemDefined()) {
            throw new BusinessLogicException("Удаление предопределенных складов запрещено");
        }
        warehouseRepository.delete(existingWarehouse);
    }
}
