package com.warehouse.demo.service.impl;

import com.warehouse.demo.dto.request.UpsertUnitRequest;
import com.warehouse.demo.dto.response.UnitListResponse;
import com.warehouse.demo.dto.response.UnitResponse;
import com.warehouse.demo.exception.EntityNotFoundException;
import com.warehouse.demo.mapper.UnitMapper;
import com.warehouse.demo.model.Unit;
import com.warehouse.demo.repository.UnitRepository;
import com.warehouse.demo.service.UnitService;
import com.warehouse.demo.utils.BeanUtils;
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
    public UnitResponse save(UpsertUnitRequest upsertUnitRequest) {
        Unit newUnit = unitRepository.save(unitMapper.upsertUnitRequestToUnit(upsertUnitRequest));
        return unitMapper.unitToUnitResponse(newUnit);
    }

    @Transactional
    @Override
    public UnitResponse update(Long id, UpsertUnitRequest upsertUnitRequest) {
        Unit existingUnit = findUnitById(id);
        BeanUtils.copyNonNullProperties(upsertUnitRequest, existingUnit);
        return unitMapper.unitToUnitResponse(unitRepository.save(existingUnit));
    }

    @Transactional
    @Override
    public void delete(Long id) {
        Unit existingUnit = findUnitById(id);
        existingUnit.setDeleted(true);
        unitMapper.unitToUnitResponse(unitRepository.save(existingUnit));
    }
}
