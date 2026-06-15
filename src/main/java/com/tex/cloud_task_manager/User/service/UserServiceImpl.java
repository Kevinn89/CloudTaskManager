package com.tex.cloud_task_manager.User.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.tex.cloud_task_manager.Security.CurrentUserService;
import com.tex.cloud_task_manager.User.UserEntity;
import com.tex.cloud_task_manager.User.UserEntityRepository;
import com.tex.cloud_task_manager.User.response_request.UserResponse;
import com.tex.cloud_task_manager.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserEntityRepository userEntityRepository;

  private final CurrentUserService currentUserService;

  @Override
  public UserEntity createUser(String name, String email, String password, String accountType) {
    UserEntity user = UserEntity.builder()
        .name(name)
        .email(email)
        .password(password)
        .createdAt(LocalDateTime.now())
        .updatedAt(null)
        .accountType(accountType)
        .build();
    return userEntityRepository.save(user);
  }

  @Override
  public List<UserResponse> getAllUsers() {
    return userEntityRepository.findAll().stream().map(UserResponse::from).toList();
  }

  @Override
  public UserResponse updateUser(String name, String password) {

    long userId = getCurrentUserId();

    UserEntity userEntity = userEntityRepository
        .findById(userId)
        .orElseThrow(
            () -> new ResourceNotFoundException("User not found for id %d".formatted(userId)));

    if (name != null && !name.isBlank())
      userEntity.setName(name);

    if (password != null && !password.isBlank())
      userEntity.setPassword(password);

    userEntity.setUpdatedAt(LocalDateTime.now());

    return UserResponse.from(userEntityRepository.save(userEntity));
  }

  private long getCurrentUserId() {
    return currentUserService.getCurrentUserId();
  }

  @Override
  public List<UserResponse> getNonOrgUsers(Long orgId) {

    List<UserResponse> orgLessUsers = userEntityRepository.findUsersNotInOrganization(orgId).stream()
        .map(UserResponse::from).toList();

    return orgLessUsers;
  }
}
