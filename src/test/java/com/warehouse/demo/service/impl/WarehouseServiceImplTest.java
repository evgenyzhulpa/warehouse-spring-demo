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
class WarehouseServiceImplTest {

    private static final long WAREHOUSE_ID = 1L;
    private static final long MISSING_WAREHOUSE_ID = 5L;
    private static final boolean SYSTEM_DEFINED = true;
    private static final boolean NOT_SYSTEM_DEFINED = false;
    private static final boolean DELETED = true;
    private static final boolean NOT_DELETED = false;
    private static final String MAIN_NAME = "Main";
    private static final String MAIN_ADDRESS = "Central yard";
    private static final String RESERVE_NAME = "Reserve";
    private static final String RESERVE_ADDRESS = "North yard";
    private static final String DELETE_FORBIDDEN_MESSAGE = "Удаление предопределенных складов запрещено";

    @Mock
    private WarehouseMapper warehouseMapper;

    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private WarehouseServiceImpl warehouseService;

    @Test
    @DisplayName("Find all returns the mapped list from the repository")
    void givenStoredWarehouses_whenFindAll_thenReturnsMappedList() {
        // given
        Warehouse storedWarehouse = warehouse(WAREHOUSE_ID, NOT_SYSTEM_DEFINED, NOT_DELETED);
        List<Warehouse> warehouses = List.of(storedWarehouse);
        WarehouseResponse warehouseResponse = response(WAREHOUSE_ID);
        List<WarehouseResponse> warehouseResponses = List.of(warehouseResponse);
        WarehouseListResponse response = new WarehouseListResponse(warehouseResponses);
        when(warehouseRepository.findAll()).thenReturn(warehouses);
        when(warehouseMapper.warehouseListToWarehouseListResponse(warehouses)).thenReturn(response);

        // when
        WarehouseListResponse result = warehouseService.findAll();

        // then
        assertSame(response, result);
    }

    @Test
    @DisplayName("Find by id returns the mapped warehouse")
    void givenStoredWarehouse_whenFindById_thenReturnsMappedResponse() {
        // given
        Warehouse warehouse = warehouse(WAREHOUSE_ID, NOT_SYSTEM_DEFINED, NOT_DELETED);
        WarehouseResponse response = response(WAREHOUSE_ID);
        Optional<Warehouse> found = Optional.of(warehouse);
        when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(found);
        when(warehouseMapper.warehouseToWarehouseResponse(warehouse)).thenReturn(response);

        // when
        WarehouseResponse result = warehouseService.findById(WAREHOUSE_ID);

        // then
        assertSame(response, result);
    }

    @Test
    @DisplayName("Find by a missing id fails with entity not found")
    void givenMissingId_whenFindById_thenEntityNotFound() {
        // given
        Optional<Warehouse> missing = Optional.empty();
        when(warehouseRepository.findById(MISSING_WAREHOUSE_ID)).thenReturn(missing);

        // when
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> warehouseService.findById(MISSING_WAREHOUSE_ID)
        );

        // then
        String message = exception.getMessage();
        String missingId = Long.toString(MISSING_WAREHOUSE_ID);
        assertTrue(message.contains(missingId));
    }

    @Test
    @DisplayName("Save maps the request and returns the mapped response")
    void givenCreateRequest_whenSave_thenReturnsMappedResponse() {
        // given
        CreateWarehouseRequest request = new CreateWarehouseRequest(MAIN_NAME, MAIN_ADDRESS);
        Warehouse mapped = new Warehouse();
        Warehouse saved = warehouse(WAREHOUSE_ID, NOT_SYSTEM_DEFINED, NOT_DELETED);
        WarehouseResponse response = response(WAREHOUSE_ID);
        when(warehouseMapper.createWarehouseRequestToWarehouse(request)).thenReturn(mapped);
        when(warehouseRepository.save(mapped)).thenReturn(saved);
        when(warehouseMapper.warehouseToWarehouseResponse(saved)).thenReturn(response);

        // when
        WarehouseResponse result = warehouseService.save(request);

        // then
        assertSame(response, result);
    }

    @Test
    @DisplayName("Update of an ordinary warehouse is saved through the mapper")
    void givenOrdinaryWarehouse_whenUpdate_thenSavesMappedWarehouse() {
        // given
        Warehouse warehouse = warehouse(WAREHOUSE_ID, NOT_SYSTEM_DEFINED, NOT_DELETED);
        UpdateWarehouseRequest request = new UpdateWarehouseRequest(RESERVE_NAME, RESERVE_ADDRESS);
        WarehouseResponse response = response(WAREHOUSE_ID);
        Optional<Warehouse> found = Optional.of(warehouse);
        when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(found);
        when(warehouseRepository.save(warehouse)).thenReturn(warehouse);
        when(warehouseMapper.warehouseToWarehouseResponse(warehouse)).thenReturn(response);

        // when
        WarehouseResponse result = warehouseService.update(WAREHOUSE_ID, request);

        // then
        assertSame(response, result);
        verify(warehouseMapper).updateWarehouseFromUpdateWarehouseRequest(request, warehouse);
        verify(warehouseRepository).save(warehouse);
    }

    @Test
    @DisplayName("Update of a system-defined warehouse is rejected and is not saved")
    void givenSystemDefinedWarehouse_whenUpdate_thenDoesNotSave() {
        // given
        Warehouse warehouse = warehouse(WAREHOUSE_ID, SYSTEM_DEFINED, NOT_DELETED);
        UpdateWarehouseRequest request = new UpdateWarehouseRequest(MAIN_NAME, MAIN_ADDRESS);
        Optional<Warehouse> found = Optional.of(warehouse);
        when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(found);

        // when
        assertThrows(
                BusinessLogicException.class,
                () -> warehouseService.update(WAREHOUSE_ID, request)
        );

        // then
        verify(warehouseRepository, never()).save(any(Warehouse.class));
    }

    @Test
    @DisplayName("An ordinary warehouse is marked deleted")
    void givenOrdinaryWarehouse_whenMarkDeleted_thenSavesMarkedWarehouse() {
        // given
        Warehouse warehouse = warehouse(WAREHOUSE_ID, NOT_SYSTEM_DEFINED, NOT_DELETED);
        WarehouseResponse response = response(WAREHOUSE_ID);
        Optional<Warehouse> found = Optional.of(warehouse);
        when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(found);
        when(warehouseRepository.save(warehouse)).thenReturn(warehouse);
        when(warehouseMapper.warehouseToWarehouseResponse(warehouse)).thenReturn(response);

        // when
        warehouseService.markAsDeleted(WAREHOUSE_ID);

        // then
        Boolean deleted = warehouse.getDeleted();
        assertTrue(Boolean.TRUE.equals(deleted));
        verify(warehouseRepository).save(warehouse);
    }

    @Test
    @DisplayName("Marking a system-defined warehouse deleted is rejected")
    void givenSystemDefinedWarehouse_whenMarkDeleted_thenDoesNotSave() {
        // given
        Warehouse warehouse = warehouse(WAREHOUSE_ID, SYSTEM_DEFINED, NOT_DELETED);
        Optional<Warehouse> found = Optional.of(warehouse);
        when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(found);

        // when
        assertThrows(BusinessLogicException.class, () -> warehouseService.markAsDeleted(WAREHOUSE_ID));

        // then
        verify(warehouseRepository, never()).save(any(Warehouse.class));
    }

    @Test
    @DisplayName("Unmarking an ordinary warehouse clears the deleted flag")
    void givenDeletedOrdinaryWarehouse_whenUnmark_thenClearsDeleted() {
        // given
        Warehouse warehouse = warehouse(WAREHOUSE_ID, NOT_SYSTEM_DEFINED, DELETED);
        WarehouseResponse response = response(WAREHOUSE_ID);
        Optional<Warehouse> found = Optional.of(warehouse);
        when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(found);
        when(warehouseRepository.save(warehouse)).thenReturn(warehouse);
        when(warehouseMapper.warehouseToWarehouseResponse(warehouse)).thenReturn(response);

        // when
        warehouseService.unmarkAsDeleted(WAREHOUSE_ID);

        // then
        Boolean deleted = warehouse.getDeleted();
        assertTrue(Boolean.FALSE.equals(deleted));
        verify(warehouseRepository).save(warehouse);
    }

    @Test
    @DisplayName("Unmarking a system-defined warehouse clears the deleted flag")
    void givenDeletedSystemDefinedWarehouse_whenUnmark_thenClearsDeleted() {
        // given
        Warehouse warehouse = warehouse(WAREHOUSE_ID, SYSTEM_DEFINED, DELETED);
        WarehouseResponse response = response(WAREHOUSE_ID);
        Optional<Warehouse> found = Optional.of(warehouse);
        when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(found);
        when(warehouseRepository.save(warehouse)).thenReturn(warehouse);
        when(warehouseMapper.warehouseToWarehouseResponse(warehouse)).thenReturn(response);

        // when
        WarehouseResponse result = warehouseService.unmarkAsDeleted(WAREHOUSE_ID);

        // then
        Boolean deleted = warehouse.getDeleted();
        assertSame(response, result);
        assertTrue(Boolean.FALSE.equals(deleted));
        verify(warehouseRepository).save(warehouse);
    }

    @Test
    @DisplayName("An ordinary warehouse is deleted from the repository")
    void givenOrdinaryWarehouse_whenDelete_thenDeletesWarehouse() {
        // given
        Warehouse warehouse = warehouse(WAREHOUSE_ID, NOT_SYSTEM_DEFINED, NOT_DELETED);
        Optional<Warehouse> found = Optional.of(warehouse);
        when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(found);

        // when
        warehouseService.delete(WAREHOUSE_ID);

        // then
        verify(warehouseRepository).delete(warehouse);
    }

    @Test
    @DisplayName("Delete of a system-defined warehouse is rejected")
    void givenSystemDefinedWarehouse_whenDelete_thenDoesNotDelete() {
        // given
        Warehouse warehouse = warehouse(WAREHOUSE_ID, SYSTEM_DEFINED, NOT_DELETED);
        Optional<Warehouse> found = Optional.of(warehouse);
        when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(found);

        // when
        BusinessLogicException exception = assertThrows(
                BusinessLogicException.class,
                () -> warehouseService.delete(WAREHOUSE_ID)
        );

        // then
        String message = exception.getMessage();
        assertTrue(message.contains(DELETE_FORBIDDEN_MESSAGE));
        verify(warehouseRepository, never()).delete(any(Warehouse.class));
    }

    private static Warehouse warehouse(Long id, boolean systemDefined, boolean deleted) {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(id);
        warehouse.setSystemDefined(systemDefined);
        warehouse.setDeleted(deleted);
        return warehouse;
    }

    private static WarehouseResponse response(Long id) {
        return new WarehouseResponse(id, MAIN_NAME, MAIN_ADDRESS, NOT_DELETED, NOT_SYSTEM_DEFINED);
    }
}
