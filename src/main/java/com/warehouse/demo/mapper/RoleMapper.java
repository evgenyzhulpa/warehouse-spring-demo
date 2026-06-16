package com.warehouse.demo.mapper;

import com.warehouse.demo.dto.request.UpsertRoleRequest;
import com.warehouse.demo.dto.response.RoleListResponse;
import com.warehouse.demo.dto.response.RoleResponse;
import com.warehouse.demo.dto.response.RoleSummaryResponse;
import com.warehouse.demo.model.Role;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
    uses = {PermissionMapper.class},
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface RoleMapper {

    Role upsertRoleRequestToRole(UpsertRoleRequest request);

    RoleResponse roleToRoleResponse(Role role);

    List<RoleSummaryResponse> rolesToRoleSummaryResponseList(List<Role> roles);

    default RoleListResponse rolesToRoleListResponse(List<Role> roles) {
        List<RoleSummaryResponse> roleResponseList = rolesToRoleSummaryResponseList(roles);
        return new RoleListResponse(roleResponseList);
    }
}
