package com.warehouse.demo.service.impl;

import com.warehouse.demo.dto.request.CreateUserRequest;
import com.warehouse.demo.dto.request.UpdateUserPasswordRequest;
import com.warehouse.demo.dto.request.UpdateUserRequest;
import com.warehouse.demo.dto.response.CurrentUserResponse;
import com.warehouse.demo.dto.response.RoleResponse;
import com.warehouse.demo.dto.response.UserListResponse;
import com.warehouse.demo.dto.response.UserResponse;
import com.warehouse.demo.dto.response.UserSummaryResponse;
import com.warehouse.demo.exception.BusinessLogicException;
import com.warehouse.demo.exception.EntityNotFoundException;
import com.warehouse.demo.mapper.UserMapper;
import com.warehouse.demo.model.PermissionCode;
import com.warehouse.demo.model.Role;
import com.warehouse.demo.model.User;
import com.warehouse.demo.repository.RoleRepository;
import com.warehouse.demo.repository.UserRepository;
import com.warehouse.demo.security.AppPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final long USER_ID = 1L;
    private static final long MISSING_USER_ID = 5L;
    private static final long ROLE_ID = 3L;
    private static final long MISSING_ROLE_ID = 9L;
    private static final boolean SYSTEM_DEFINED = true;
    private static final boolean NOT_SYSTEM_DEFINED = false;
    private static final boolean DELETED = true;
    private static final boolean NOT_DELETED = false;
    private static final boolean ENABLED = true;
    private static final boolean OLD_PASSWORD_MATCHES = true;
    private static final boolean OLD_PASSWORD_DOES_NOT_MATCH = false;
    private static final String USERNAME = "storekeeper";
    private static final String NEW_USERNAME = "manager";
    private static final String PASSWORD = "secret";
    private static final String NEW_PASSWORD = "next-secret";
    private static final String PASSWORD_HASH = "hash";
    private static final String NEW_PASSWORD_HASH = "next-hash";
    private static final String LAST_NAME = "Ivanov";
    private static final String FIRST_NAME = "Ivan";
    private static final String MIDDLE_NAME = "Ivanovich";
    private static final String DELETE_FORBIDDEN_MESSAGE = "Удаление предопределенных пользователей запрещено";
    private static final String WRONG_PASSWORD_MESSAGE = "Старый пароль указан неверно";

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("Find all returns the mapped list from the repository")
    void givenStoredUsers_whenFindAll_thenReturnsMappedList() {
        // given
        User storedUser = user(USER_ID, NOT_SYSTEM_DEFINED, NOT_DELETED);
        List<User> users = List.of(storedUser);
        UserSummaryResponse summary = summary(USER_ID);
        List<UserSummaryResponse> summaries = List.of(summary);
        UserListResponse response = new UserListResponse(summaries);
        when(userRepository.findAll()).thenReturn(users);
        when(userMapper.usersToUserListResponse(users)).thenReturn(response);

        // when
        UserListResponse result = userService.findAll();

        // then
        assertSame(response, result);
    }

    @Test
    @DisplayName("Current user data returns the mapped principal")
    void givenPrincipal_whenGetCurrentUserData_thenReturnsMappedResponse() {
        // given
        User user = user(USER_ID, NOT_SYSTEM_DEFINED, NOT_DELETED);
        AppPrincipal principal = new AppPrincipal(user);
        Set<PermissionCode> permissionCodes = Set.of(PermissionCode.USERS_READ);
        CurrentUserResponse response = new CurrentUserResponse(USER_ID, USERNAME, permissionCodes);
        when(userMapper.userToCurrentUserResponse(user)).thenReturn(response);

        // when
        CurrentUserResponse result = userService.getCurrentUserData(principal);

        // then
        assertSame(response, result);
    }

    @Test
    @DisplayName("Find by id returns the mapped user")
    void givenStoredUser_whenFindById_thenReturnsMappedResponse() {
        // given
        User user = user(USER_ID, NOT_SYSTEM_DEFINED, NOT_DELETED);
        UserResponse response = response(USER_ID);
        Optional<User> found = Optional.of(user);
        when(userRepository.findById(USER_ID)).thenReturn(found);
        when(userMapper.userToUserResponse(user)).thenReturn(response);

        // when
        UserResponse result = userService.findById(USER_ID);

        // then
        assertSame(response, result);
    }

    @Test
    @DisplayName("Find by a missing id fails with entity not found")
    void givenMissingId_whenFindById_thenEntityNotFound() {
        // given
        Optional<User> missing = Optional.empty();
        when(userRepository.findById(MISSING_USER_ID)).thenReturn(missing);

        // when
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> userService.findById(MISSING_USER_ID)
        );

        // then
        String message = exception.getMessage();
        String missingId = Long.toString(MISSING_USER_ID);
        assertTrue(message.contains(missingId));
    }

    @Test
    @DisplayName("Save encodes the password, attaches roles and returns the mapped response")
    void givenCreateRequest_whenSave_thenReturnsMappedResponse() {
        // given
        List<Long> roleIds = List.of(ROLE_ID);
        CreateUserRequest request = new CreateUserRequest(
                USERNAME, PASSWORD, LAST_NAME, FIRST_NAME, MIDDLE_NAME, roleIds);
        User mapped = new User();
        Role role = role(ROLE_ID);
        List<Role> roles = List.of(role);
        UserResponse response = response(USER_ID);
        when(userMapper.createUserRequestToUser(request)).thenReturn(mapped);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_HASH);
        when(roleRepository.findAllById(roleIds)).thenReturn(roles);
        when(userRepository.save(mapped)).thenReturn(mapped);
        when(userMapper.userToUserResponse(mapped)).thenReturn(response);

        // when
        UserResponse result = userService.save(request);

        // then
        String passwordHash = mapped.getPasswordHash();
        Set<Role> assigned = mapped.getRoles();
        boolean roleAssigned = assigned.contains(role);
        assertSame(response, result);
        assertEquals(PASSWORD_HASH, passwordHash);
        assertTrue(roleAssigned);
    }

    @Test
    @DisplayName("Save fails when a role does not exist and the user is not stored")
    void givenMissingRole_whenSave_thenDoesNotSave() {
        // given
        List<Long> roleIds = List.of(MISSING_ROLE_ID);
        CreateUserRequest request = new CreateUserRequest(
                USERNAME, PASSWORD, LAST_NAME, FIRST_NAME, MIDDLE_NAME, roleIds);
        User mapped = new User();
        List<Role> noRoles = List.of();
        when(userMapper.createUserRequestToUser(request)).thenReturn(mapped);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_HASH);
        when(roleRepository.findAllById(roleIds)).thenReturn(noRoles);

        // when
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> userService.save(request)
        );

        // then
        String message = exception.getMessage();
        String missingId = Long.toString(MISSING_ROLE_ID);
        assertTrue(message.contains(missingId));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Update of an ordinary user is saved with the selected roles")
    void givenOrdinaryUser_whenUpdate_thenSavesMappedUser() {
        // given
        User user = user(USER_ID, NOT_SYSTEM_DEFINED, NOT_DELETED);
        Role role = role(ROLE_ID);
        List<Long> roleIds = List.of(ROLE_ID);
        List<Role> roles = List.of(role);
        UpdateUserRequest request = new UpdateUserRequest(
                NEW_USERNAME, ENABLED, LAST_NAME, FIRST_NAME, MIDDLE_NAME, roleIds);
        UserResponse response = response(USER_ID);
        Optional<User> found = Optional.of(user);
        when(userRepository.findById(USER_ID)).thenReturn(found);
        when(roleRepository.findAllById(roleIds)).thenReturn(roles);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.userToUserResponse(user)).thenReturn(response);

        // when
        UserResponse result = userService.update(USER_ID, request);

        // then
        Set<Role> assigned = user.getRoles();
        boolean roleAssigned = assigned.contains(role);
        assertSame(response, result);
        assertTrue(roleAssigned);
        verify(userMapper).updateUserFromUpdateUserRequest(request, user);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Update without roles keeps the user and does not load roles")
    void givenOrdinaryUserWithoutRoles_whenUpdate_thenSavesUser() {
        // given
        User user = user(USER_ID, NOT_SYSTEM_DEFINED, NOT_DELETED);
        UpdateUserRequest request = new UpdateUserRequest(
                NEW_USERNAME, ENABLED, LAST_NAME, FIRST_NAME, MIDDLE_NAME, null);
        UserResponse response = response(USER_ID);
        Optional<User> found = Optional.of(user);
        when(userRepository.findById(USER_ID)).thenReturn(found);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.userToUserResponse(user)).thenReturn(response);

        // when
        UserResponse result = userService.update(USER_ID, request);

        // then
        assertSame(response, result);
        verify(roleRepository, never()).findAllById(any());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Update of a system-defined user is rejected and is not saved")
    void givenSystemDefinedUser_whenUpdate_thenDoesNotSave() {
        // given
        User user = user(USER_ID, SYSTEM_DEFINED, NOT_DELETED);
        List<Long> roleIds = List.of(ROLE_ID);
        UpdateUserRequest request = new UpdateUserRequest(
                USERNAME, ENABLED, LAST_NAME, FIRST_NAME, MIDDLE_NAME, roleIds);
        Optional<User> found = Optional.of(user);
        when(userRepository.findById(USER_ID)).thenReturn(found);

        // when
        assertThrows(
                BusinessLogicException.class,
                () -> userService.update(USER_ID, request)
        );

        // then
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Update fails when a role does not exist and the user is not stored")
    void givenMissingRole_whenUpdate_thenDoesNotSave() {
        // given
        User user = user(USER_ID, NOT_SYSTEM_DEFINED, NOT_DELETED);
        List<Long> roleIds = List.of(MISSING_ROLE_ID);
        List<Role> noRoles = List.of();
        UpdateUserRequest request = new UpdateUserRequest(
                USERNAME, ENABLED, LAST_NAME, FIRST_NAME, MIDDLE_NAME, roleIds);
        Optional<User> found = Optional.of(user);
        when(userRepository.findById(USER_ID)).thenReturn(found);
        when(roleRepository.findAllById(roleIds)).thenReturn(noRoles);

        // when
        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> userService.update(USER_ID, request)
        );

        // then
        String message = exception.getMessage();
        String missingId = Long.toString(MISSING_ROLE_ID);
        assertTrue(message.contains(missingId));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Password change stores the encoded new password")
    void givenMatchingOldPassword_whenChangePassword_thenSavesNewHash() {
        // given
        User user = user(USER_ID, NOT_SYSTEM_DEFINED, NOT_DELETED);
        UpdateUserPasswordRequest request = new UpdateUserPasswordRequest(PASSWORD, NEW_PASSWORD);
        UserResponse response = response(USER_ID);
        Optional<User> found = Optional.of(user);
        when(userRepository.findById(USER_ID)).thenReturn(found);
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(OLD_PASSWORD_MATCHES);
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(NEW_PASSWORD_HASH);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.userToUserResponse(user)).thenReturn(response);

        // when
        UserResponse result = userService.changePassword(USER_ID, request);

        // then
        String passwordHash = user.getPasswordHash();
        assertSame(response, result);
        assertEquals(NEW_PASSWORD_HASH, passwordHash);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Password change of a system-defined user is rejected")
    void givenSystemDefinedUser_whenChangePassword_thenDoesNotSave() {
        // given
        User user = user(USER_ID, SYSTEM_DEFINED, NOT_DELETED);
        UpdateUserPasswordRequest request = new UpdateUserPasswordRequest(PASSWORD, NEW_PASSWORD);
        Optional<User> found = Optional.of(user);
        when(userRepository.findById(USER_ID)).thenReturn(found);

        // when
        assertThrows(
                BusinessLogicException.class,
                () -> userService.changePassword(USER_ID, request)
        );

        // then
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Password change with a wrong old password is rejected")
    void givenWrongOldPassword_whenChangePassword_thenDoesNotSave() {
        // given
        User user = user(USER_ID, NOT_SYSTEM_DEFINED, NOT_DELETED);
        UpdateUserPasswordRequest request = new UpdateUserPasswordRequest(PASSWORD, NEW_PASSWORD);
        Optional<User> found = Optional.of(user);
        when(userRepository.findById(USER_ID)).thenReturn(found);
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(OLD_PASSWORD_DOES_NOT_MATCH);

        // when
        BusinessLogicException exception = assertThrows(
                BusinessLogicException.class,
                () -> userService.changePassword(USER_ID, request)
        );

        // then
        String message = exception.getMessage();
        assertTrue(message.contains(WRONG_PASSWORD_MESSAGE));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("An ordinary user is marked deleted")
    void givenOrdinaryUser_whenMarkDeleted_thenSavesMarkedUser() {
        // given
        User user = user(USER_ID, NOT_SYSTEM_DEFINED, NOT_DELETED);
        UserResponse response = response(USER_ID);
        Optional<User> found = Optional.of(user);
        when(userRepository.findById(USER_ID)).thenReturn(found);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.userToUserResponse(user)).thenReturn(response);

        // when
        userService.markAsDeleted(USER_ID);

        // then
        Boolean deleted = user.getDeleted();
        assertTrue(Boolean.TRUE.equals(deleted));
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Marking a system-defined user deleted is rejected")
    void givenSystemDefinedUser_whenMarkDeleted_thenDoesNotSave() {
        // given
        User user = user(USER_ID, SYSTEM_DEFINED, NOT_DELETED);
        Optional<User> found = Optional.of(user);
        when(userRepository.findById(USER_ID)).thenReturn(found);

        // when
        assertThrows(BusinessLogicException.class, () -> userService.markAsDeleted(USER_ID));

        // then
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Unmarking an ordinary user clears the deleted flag")
    void givenDeletedOrdinaryUser_whenUnmark_thenClearsDeleted() {
        // given
        User user = user(USER_ID, NOT_SYSTEM_DEFINED, DELETED);
        UserResponse response = response(USER_ID);
        Optional<User> found = Optional.of(user);
        when(userRepository.findById(USER_ID)).thenReturn(found);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.userToUserResponse(user)).thenReturn(response);

        // when
        userService.unmarkAsDeleted(USER_ID);

        // then
        Boolean deleted = user.getDeleted();
        assertTrue(Boolean.FALSE.equals(deleted));
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Unmarking a system-defined user clears the deleted flag")
    void givenDeletedSystemDefinedUser_whenUnmark_thenClearsDeleted() {
        // given
        User user = user(USER_ID, SYSTEM_DEFINED, DELETED);
        UserResponse response = response(USER_ID);
        Optional<User> found = Optional.of(user);
        when(userRepository.findById(USER_ID)).thenReturn(found);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.userToUserResponse(user)).thenReturn(response);

        // when
        UserResponse result = userService.unmarkAsDeleted(USER_ID);

        // then
        Boolean deleted = user.getDeleted();
        assertSame(response, result);
        assertTrue(Boolean.FALSE.equals(deleted));
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("An ordinary user is deleted from the repository")
    void givenOrdinaryUser_whenDelete_thenDeletesUser() {
        // given
        User user = user(USER_ID, NOT_SYSTEM_DEFINED, NOT_DELETED);
        Optional<User> found = Optional.of(user);
        when(userRepository.findById(USER_ID)).thenReturn(found);

        // when
        userService.delete(USER_ID);

        // then
        verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("Delete of a system-defined user is rejected")
    void givenSystemDefinedUser_whenDelete_thenDoesNotDelete() {
        // given
        User user = user(USER_ID, SYSTEM_DEFINED, NOT_DELETED);
        Optional<User> found = Optional.of(user);
        when(userRepository.findById(USER_ID)).thenReturn(found);

        // when
        BusinessLogicException exception = assertThrows(
                BusinessLogicException.class,
                () -> userService.delete(USER_ID)
        );

        // then
        String message = exception.getMessage();
        assertTrue(message.contains(DELETE_FORBIDDEN_MESSAGE));
        verify(userRepository, never()).delete(any(User.class));
    }

    private static User user(Long id, boolean systemDefined, boolean deleted) {
        User user = new User();
        user.setId(id);
        user.setUsername(USERNAME);
        user.setPasswordHash(PASSWORD_HASH);
        user.setEnabled(ENABLED);
        user.setLastName(LAST_NAME);
        user.setFirstName(FIRST_NAME);
        user.setMiddleName(MIDDLE_NAME);
        user.setSystemDefined(systemDefined);
        user.setDeleted(deleted);
        return user;
    }

    private static Role role(Long id) {
        Role role = new Role();
        role.setId(id);
        role.setDeleted(NOT_DELETED);
        role.setSystemDefined(NOT_SYSTEM_DEFINED);
        return role;
    }

    private static UserResponse response(Long id) {
        List<RoleResponse> roles = List.of();
        return new UserResponse(
                id,
                USERNAME,
                ENABLED,
                LAST_NAME,
                FIRST_NAME,
                MIDDLE_NAME,
                NOT_DELETED,
                NOT_SYSTEM_DEFINED,
                roles
        );
    }

    private static UserSummaryResponse summary(Long id) {
        return new UserSummaryResponse(
                id,
                USERNAME,
                ENABLED,
                NOT_DELETED,
                NOT_SYSTEM_DEFINED,
                LAST_NAME,
                FIRST_NAME,
                MIDDLE_NAME
        );
    }
}
