package com.warehouse.demo.service.impl;

import com.warehouse.demo.dto.request.CreateUnitRequest;
import com.warehouse.demo.dto.request.UpdateUnitRequest;
import com.warehouse.demo.dto.response.UnitListResponse;
import com.warehouse.demo.dto.response.UnitResponse;
import com.warehouse.demo.exception.BusinessLogicException;
import com.warehouse.demo.exception.EntityNotFoundException;
import com.warehouse.demo.mapper.UnitMapper;
import com.warehouse.demo.model.Unit;
import com.warehouse.demo.repository.UnitRepository;
import com.warehouse.demo.service.UnitService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;

@Service
@RequiredArgsConstructor
public class UnitServiceImpl implements UnitService {

    private final UnitMapper unitMapper;
    private final UnitRepository unitRepository;

    @Transactional(readOnly = true)
    @Override
    public UnitListResponse findAll() {
        return unitMapper.unitListToUnitListResponse(
                unitRepository.findAll());
    }

    @Transactional(readOnly = true)
    @Override
    public UnitResponse findById(Long id) {
        return unitMapper.unitToUnitResponse(findUnitById(id));
    }

    private Unit findUnitById(Long id) {
        return unitRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        MessageFormat.format("Единица измерения с ID {0} не найдена!", id)
                ));
    }

    @Transactional
    @Override
    public UnitResponse save(CreateUnitRequest createUnitRequest) {
        Unit newUnit = unitRepository.save(unitMapper.createUnitRequestToUnit(createUnitRequest));
        return unitMapper.unitToUnitResponse(newUnit);
    }

    @Transactional
    @Override
    public UnitResponse update(Long id, UpdateUnitRequest updateUnitRequest) {
        Unit existingUnit = findUnitById(id);
        if (existingUnit.getSystemDefined()) {
            throw new BusinessLogicException("Редактирование предопределенных единиц измерения запрещено");
        }
        unitMapper.updateUnitFromUpdateUnitRequest(updateUnitRequest, existingUnit);
        return unitMapper.unitToUnitResponse(unitRepository.save(existingUnit));
    }

    @Transactional
    @Override
    public UnitResponse markAsDeleted(Long id) {
        return setDeletedValue(id, true);
    }

    private UnitResponse setDeletedValue(Long id, boolean isDeleted) {
        Unit existingUnit = findUnitById(id);
        if (isDeleted && existingUnit.getSystemDefined()) {
            throw new BusinessLogicException("Редактирование предопределенных единиц измерения запрещено");
        }
        existingUnit.setDeleted(isDeleted);
        return unitMapper.unitToUnitResponse(unitRepository.save(existingUnit));
    }

    @Transactional
    @Override
    public UnitResponse unmarkAsDeleted(Long id) {
        return setDeletedValue(id, false);
    }

    @Transactional
    @Override
    public void delete(Long id) {
        Unit existingUnit = findUnitById(id);
        if (existingUnit.getSystemDefined()) {
            throw new BusinessLogicException("Удаление предопределенных единиц измерения запрещено");
        }
        unitRepository.delete(existingUnit);
    }
}
