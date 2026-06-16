package com.warehouse.demo.mapper;

import com.warehouse.demo.dto.request.UpsertUnitRequest;
import com.warehouse.demo.dto.response.UnitListResponse;
import com.warehouse.demo.dto.response.UnitResponse;
import com.warehouse.demo.model.Unit;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface UnitMapper {

    Unit upsertUnitRequestToUnit(UpsertUnitRequest upsertUnitRequest);

    UnitResponse unitToUnitResponse(Unit unit);

    List<UnitResponse> unitListToUnitResponseList(List<Unit> unitList);

    default UnitListResponse unitListToUnitListResponse(List<Unit> unitList) {
        List<UnitResponse> unitResponseList = unitListToUnitResponseList(unitList);
        return new UnitListResponse(unitResponseList);
    }

}
