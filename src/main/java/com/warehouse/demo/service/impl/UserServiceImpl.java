package com.warehouse.demo.service.impl;

import com.warehouse.demo.dto.request.CreateUserRequest;
import com.warehouse.demo.dto.request.UpdateUserPasswordRequest;
import com.warehouse.demo.dto.request.UpdateUserRequest;
import com.warehouse.demo.dto.response.CurrentUserResponse;
import com.warehouse.demo.dto.response.UserListResponse;
import com.warehouse.demo.dto.response.UserResponse;
import com.warehouse.demo.exception.BusinessLogicException;
import com.warehouse.demo.exception.EntityNotFoundException;
import com.warehouse.demo.mapper.UserMapper;
import com.warehouse.demo.model.Role;
import com.warehouse.demo.model.User;
import com.warehouse.demo.repository.RoleRepository;
import com.warehouse.demo.repository.UserRepository;
import com.warehouse.demo.security.AppPrincipal;
import com.warehouse.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    @Override
    public UserListResponse findAll() {
        List<User> users = userRepository.findAll();
        return userMapper.usersToUserListResponse(users);
    }

    @Transactional(readOnly = true)
    @Override
    public CurrentUserResponse getCurrentUserData(AppPrincipal appPrincipal) {
        User currentUser = appPrincipal.getUser();
        return userMapper.userToCurrentUserResponse(currentUser);
    }

    @Transactional(readOnly = true)
    @Override
    public UserResponse findById(Long id) {
        return userMapper.userToUserResponse(findUserById(id));
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        MessageFormat.format("Пользователь с id {0} не найден", id)
                ));
    }

    @Transactional
    @Override
    public UserResponse save(CreateUserRequest createUserRequest) {
        User newUser = userMapper.createUserRequestToUser(createUserRequest);
        newUser.setPasswordHash(passwordEncoder.encode(createUserRequest.password()));
        Set<Role> roles = findRolesById(
                createUserRequest.rolesIdentifiers());
        newUser.setRoles(roles);
        return userMapper.userToUserResponse(userRepository.save(newUser));
    }

    private Set<Role> findRolesById(List<Long> rolesIdentifiers) {

        Set<Role> roles = new HashSet<>(roleRepository.findAllById(rolesIdentifiers));
        if (rolesIdentifiers.size() == roles.size()) {
            return roles;
        }

        List<Long> foundIds = roles
                .stream()
                .map(Role::getId)
                .toList();

        List<Long> missingIds = rolesIdentifiers
                .stream()
                .filter(id -> !foundIds.contains(id))
                .toList();

        throw new EntityNotFoundException(
                MessageFormat.format("Не удалось найти роли c id: {0}", missingIds));
    }

    @Transactional
    @Override
    public UserResponse update(Long id, UpdateUserRequest updateUserRequest) {
        User existingUser = findUserById(id);
        if (existingUser.getSystemDefined()) {
            throw new BusinessLogicException("Редактирование предопределенных пользователей запрещено");
        }

        userMapper.updateUserFromUpdateUserRequest(updateUserRequest, existingUser);

        List<Long> rolesIdentifiers = updateUserRequest.rolesIdentifiers();
        if (rolesIdentifiers != null) {
            Set<Role> roles = findRolesById(rolesIdentifiers);
            existingUser.setRoles(roles);
        }

        return userMapper.userToUserResponse(userRepository.save(existingUser));
    }

    @Transactional
    @Override
    public UserResponse changePassword(Long id, UpdateUserPasswordRequest updateUserPasswordRequest) {
        User existingUser = findUserById(id);
        if (existingUser.getSystemDefined()) {
            throw new BusinessLogicException("Редактирование предопределенных пользователей запрещено");
        }
        String oldPassword = updateUserPasswordRequest.oldPassword();
        if (!passwordEncoder.matches(oldPassword, existingUser.getPasswordHash())) {
            throw new BusinessLogicException("Старый пароль указан неверно");
        }

        existingUser.setPasswordHash(passwordEncoder.encode(updateUserPasswordRequest.newPassword()));
        return userMapper.userToUserResponse(userRepository.save(existingUser));
    }

    @Transactional
    @Override
    public UserResponse markAsDeleted(Long id) {
        return setDeletedValue(id, true);
    }

    private UserResponse setDeletedValue(Long id, boolean isDeleted) {
        User existingUser = findUserById(id);
        if (isDeleted && existingUser.getSystemDefined()) {
            throw new BusinessLogicException("Редактирование предопределенных пользователей запрещено");
        }
        existingUser.setDeleted(isDeleted);
        return userMapper.userToUserResponse(userRepository.save(existingUser));
    }

    @Transactional
    @Override
    public UserResponse unmarkAsDeleted(Long id) {
        return setDeletedValue(id, false);
    }

    @Transactional
    @Override
    public void delete(Long id) {
        User existingUser = findUserById(id);
        if (existingUser.getSystemDefined()) {
            throw new BusinessLogicException("Удаление предопределенных пользователей запрещено");
        }
        userRepository.delete(existingUser);
    }
}
