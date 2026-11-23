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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnitServiceImplTest {

    private static final long UNIT_ID = 1L;
    private static final long OTHER_UNIT_ID = 2L;
    private static final long MISSING_UNIT_ID = 5L;
    private static final boolean SYSTEM_DEFINED = true;
    private static final boolean NOT_SYSTEM_DEFINED = false;
    private static final boolean DELETED = true;
    private static final boolean NOT_DELETED = false;
    private static final String PIECE_NAME = "Piece";
    private static final String PIECE_CODE = "796";
    private static final String KILOGRAM_NAME = "Kilogram";
    private static final String KILOGRAM_CODE = "166";
    private static final String DELETE_FORBIDDEN_MESSAGE = "Удаление предопределенных единиц измерения запрещено";

    @Mock
    private UnitMapper unitMapper;

    @Mock
    private UnitRepository unitRepository;

    @InjectMocks
    private UnitServiceImpl unitService;

    @Test
    @DisplayName("Find all returns the mapped list from the repository")
    void givenStoredUnits_whenFindAll_thenReturnsMappedList() {
        // given
        Unit storedUnit = unit(UNIT_ID, NOT_SYSTEM_DEFINED, NOT_DELETED);
        List<Unit> units = List.of(storedUnit);
        UnitResponse unitResponse = response(UNIT_ID);
        List<UnitResponse> unitResponses = List.of(unitResponse);
        UnitListResponse response = new UnitListResponse(unitResponses);
        when(unitRepository.findAll()).thenReturn(units);
        when(unitMapper.unitListToUnitListResponse(units)).thenReturn(response);

        // when
        UnitListResponse result = unitService.findAll();

        // then
        assertSame(response, result);
    }

    @Test
    @DisplayName("Find by a missing id fails with entity not found")
    void givenMissingId_whenFindById_thenEntityNotFound() {
        // given
        Optional<Unit> missing = Optional.empty();
        when(unitRepository.findById(MISSING_UNIT_ID)).thenReturn(missing);

        // when
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> unitService.findById(MISSING_UNIT_ID)
        );

        // then
        String message = exception.getMessage();
        String missingId = Long.toString(MISSING_UNIT_ID);
        assertTrue(message.contains(missingId));
    }

    @Test
    @DisplayName("Save maps the request and returns the mapped response")
    void givenCreateRequest_whenSave_thenReturnsMappedResponse() {
        // given
        CreateUnitRequest request = new CreateUnitRequest(PIECE_NAME, PIECE_CODE, null);
        Unit mapped = new Unit();
        Unit saved = unit(UNIT_ID, NOT_SYSTEM_DEFINED, NOT_DELETED);
        UnitResponse response = response(UNIT_ID);
        when(unitMapper.createUnitRequestToUnit(request)).thenReturn(mapped);
        when(unitRepository.save(mapped)).thenReturn(saved);
        when(unitMapper.unitToUnitResponse(saved)).thenReturn(response);

        // when
        UnitResponse result = unitService.save(request);

        // then
        assertSame(response, result);
    }

    @Test
    @DisplayName("Update of an ordinary unit is saved through the mapper")
    void givenOrdinaryUnit_whenUpdate_thenSavesMappedUnit() {
        // given
        Unit unit = unit(UNIT_ID, NOT_SYSTEM_DEFINED, NOT_DELETED);
        UpdateUnitRequest request = new UpdateUnitRequest(KILOGRAM_NAME, KILOGRAM_CODE, null);
        UnitResponse response = response(UNIT_ID);
        Optional<Unit> found = Optional.of(unit);
        when(unitRepository.findById(UNIT_ID)).thenReturn(found);
        when(unitRepository.save(unit)).thenReturn(unit);
        when(unitMapper.unitToUnitResponse(unit)).thenReturn(response);

        // when
        UnitResponse result = unitService.update(UNIT_ID, request);

        // then
        assertSame(response, result);
        verify(unitMapper).updateUnitFromUpdateUnitRequest(request, unit);
        verify(unitRepository).save(unit);
    }

    @Test
    @DisplayName("Update of a system-defined unit is rejected and is not saved")
    void givenSystemDefinedUnit_whenUpdate_thenDoesNotSave() {
        // given
        Unit unit = unit(UNIT_ID, SYSTEM_DEFINED, NOT_DELETED);
        UpdateUnitRequest request = new UpdateUnitRequest(PIECE_NAME, PIECE_CODE, null);
        Optional<Unit> found = Optional.of(unit);
        when(unitRepository.findById(UNIT_ID)).thenReturn(found);

        // when
        assertThrows(
                BusinessLogicException.class,
                () -> unitService.update(UNIT_ID, request)
        );

        // then
        verify(unitRepository, never()).save(any());
    }

    @Test
    @DisplayName("An ordinary unit is marked deleted")
    void givenOrdinaryUnit_whenMarkDeleted_thenSavesMarkedUnit() {
        // given
        Unit unit = unit(UNIT_ID, NOT_SYSTEM_DEFINED, NOT_DELETED);
        UnitResponse response = response(UNIT_ID);
        Optional<Unit> found = Optional.of(unit);
        when(unitRepository.findById(UNIT_ID)).thenReturn(found);
        when(unitRepository.save(unit)).thenReturn(unit);
        when(unitMapper.unitToUnitResponse(unit)).thenReturn(response);

        // when
        unitService.markAsDeleted(UNIT_ID);

        // then
        Boolean deleted = unit.getDeleted();
        assertTrue(Boolean.TRUE.equals(deleted));
        verify(unitRepository).save(unit);
    }

    @Test
    @DisplayName("Marking a system-defined unit deleted is rejected")
    void givenSystemDefinedUnit_whenMarkDeleted_thenDoesNotSave() {
        // given
        Unit unit = unit(UNIT_ID, SYSTEM_DEFINED, NOT_DELETED);
        Optional<Unit> found = Optional.of(unit);
        when(unitRepository.findById(UNIT_ID)).thenReturn(found);

        // when
        assertThrows(BusinessLogicException.class, () -> unitService.markAsDeleted(UNIT_ID));

        // then
        verify(unitRepository, never()).save(any());
    }

    @Test
    @DisplayName("Unmarking an ordinary unit clears the deleted flag")
    void givenDeletedOrdinaryUnit_whenUnmark_thenClearsDeleted() {
        // given
        Unit unit = unit(OTHER_UNIT_ID, NOT_SYSTEM_DEFINED, DELETED);
        UnitResponse response = response(OTHER_UNIT_ID);
        Optional<Unit> found = Optional.of(unit);
        when(unitRepository.findById(OTHER_UNIT_ID)).thenReturn(found);
        when(unitRepository.save(unit)).thenReturn(unit);
        when(unitMapper.unitToUnitResponse(unit)).thenReturn(response);

        // when
        unitService.unmarkAsDeleted(OTHER_UNIT_ID);

        // then
        Boolean deleted = unit.getDeleted();
        assertTrue(Boolean.FALSE.equals(deleted));
        verify(unitRepository).save(unit);
    }

    @Test
    @DisplayName("Unmarking a system-defined unit clears the deleted flag")
    void givenDeletedSystemDefinedUnit_whenUnmark_thenClearsDeleted() {
        // given
        Unit unit = unit(UNIT_ID, SYSTEM_DEFINED, DELETED);
        UnitResponse response = response(UNIT_ID);
        Optional<Unit> found = Optional.of(unit);
        when(unitRepository.findById(UNIT_ID)).thenReturn(found);
        when(unitRepository.save(unit)).thenReturn(unit);
        when(unitMapper.unitToUnitResponse(unit)).thenReturn(response);

        // when
        UnitResponse result = unitService.unmarkAsDeleted(UNIT_ID);

        // then
        Boolean deleted = unit.getDeleted();
        assertSame(response, result);
        assertTrue(Boolean.FALSE.equals(deleted));
        verify(unitRepository).save(unit);
    }

    @Test
    @DisplayName("An ordinary unit is deleted from the repository")
    void givenOrdinaryUnit_whenDelete_thenDeletesUnit() {
        // given
        Unit unit = unit(UNIT_ID, NOT_SYSTEM_DEFINED, NOT_DELETED);
        Optional<Unit> found = Optional.of(unit);
        when(unitRepository.findById(UNIT_ID)).thenReturn(found);

        // when
        unitService.delete(UNIT_ID);

        // then
        verify(unitRepository).delete(unit);
    }

    @Test
    @DisplayName("Delete of a system-defined unit is rejected")
    void givenSystemDefinedUnit_whenDelete_thenDoesNotDelete() {
        // given
        Unit unit = unit(UNIT_ID, SYSTEM_DEFINED, NOT_DELETED);
        Optional<Unit> found = Optional.of(unit);
        when(unitRepository.findById(UNIT_ID)).thenReturn(found);

        // when
        BusinessLogicException exception = assertThrows(
                BusinessLogicException.class,
                () -> unitService.delete(UNIT_ID)
        );

        // then
        String message = exception.getMessage();
        assertTrue(message.contains(DELETE_FORBIDDEN_MESSAGE));
        verify(unitRepository, never()).delete(any());
    }

    private static Unit unit(Long id, boolean systemDefined, boolean deleted) {
        Unit unit = new Unit();
        unit.setId(id);
        unit.setSystemDefined(systemDefined);
        unit.setDeleted(deleted);
        return unit;
    }

    private static UnitResponse response(Long id) {
        return new UnitResponse(id, PIECE_NAME, PIECE_CODE, null, NOT_DELETED, NOT_SYSTEM_DEFINED);
    }
}
