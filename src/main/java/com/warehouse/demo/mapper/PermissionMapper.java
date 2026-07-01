package com.warehouse.demo.mapper;

import com.warehouse.demo.dto.response.PermissionListResponse;
import com.warehouse.demo.dto.response.PermissionResponse;
import com.warehouse.demo.model.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PermissionMapper {

    PermissionResponse permissionToPermissionResponse(Permission permission);

    List<PermissionResponse> permissionsToPermissionResponseList(List<Permission> permissions);

    default PermissionListResponse permissionsToPermissionListResponse(List<Permission> permissions) {
        List<PermissionResponse> permissionResponseList = permissionsToPermissionResponseList(permissions);
        return new PermissionListResponse(permissionResponseList);
    }
}
