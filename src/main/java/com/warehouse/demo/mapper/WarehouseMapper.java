package com.warehouse.demo.mapper;

import com.warehouse.demo.dto.request.CreateWarehouseRequest;
import com.warehouse.demo.dto.request.UpdateWarehouseRequest;
import com.warehouse.demo.dto.response.WarehouseListResponse;
import com.warehouse.demo.dto.response.WarehouseResponse;
import com.warehouse.demo.model.Warehouse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface WarehouseMapper {

    Warehouse createWarehouseRequestToWarehouse(CreateWarehouseRequest request);

    void updateWarehouseFromUpdateWarehouseRequest(UpdateWarehouseRequest request, @MappingTarget Warehouse warehouse);

    WarehouseResponse warehouseToWarehouseResponse(Warehouse warehouse);

    List<WarehouseResponse> warehouseListToWarehouseResponseList(List<Warehouse> warehouseList);

    default WarehouseListResponse warehouseListToWarehouseListResponse(List<Warehouse> warehouseList) {
        List<WarehouseResponse> warehouseResponseList = warehouseListToWarehouseResponseList(warehouseList);
        return new WarehouseListResponse(warehouseResponseList);
    }

}
