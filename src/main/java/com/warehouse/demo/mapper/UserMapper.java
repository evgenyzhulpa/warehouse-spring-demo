package com.warehouse.demo.mapper;

import com.warehouse.demo.dto.request.CreateUserRequest;
import com.warehouse.demo.dto.request.UpdateUserRequest;
import com.warehouse.demo.dto.response.CurrentUserResponse;
import com.warehouse.demo.dto.response.UserListResponse;
import com.warehouse.demo.dto.response.UserResponse;
import com.warehouse.demo.dto.response.UserSummaryResponse;
import com.warehouse.demo.model.Permission;
import com.warehouse.demo.model.PermissionCode;
import com.warehouse.demo.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring",
        uses = {RoleMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {

    User createUserRequestToUser(CreateUserRequest request);

    void updateUserFromUpdateUserRequest(UpdateUserRequest request, @MappingTarget User user);

    UserResponse userToUserResponse(User user);

    List<UserSummaryResponse> usersToUserSummaryResponseList(List<User> users);

    default CurrentUserResponse userToCurrentUserResponse(User user) {
        Set<PermissionCode> permissionCodes = user.getRoles()
                .stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getCode)
                .collect(Collectors.toSet());
        return new CurrentUserResponse(user.getId(), user.getUsername(), permissionCodes);
    }

    default UserListResponse usersToUserListResponse(List<User> users) {
        List<UserSummaryResponse> userResponseList = usersToUserSummaryResponseList(users);
        return new UserListResponse(userResponseList);
    }
}
