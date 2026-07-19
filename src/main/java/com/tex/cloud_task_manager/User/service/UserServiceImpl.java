package com.tex.cloud_task_manager.User.service;

import com.tex.cloud_task_manager.Security.CurrentUserService;
import com.tex.cloud_task_manager.User.UserEntity;
import com.tex.cloud_task_manager.User.UserEntityRepository;
import com.tex.cloud_task_manager.User.response_request.UserResponse;
import com.tex.cloud_task_manager.common.exception.ResourceNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

  private final UserEntityRepository userEntityRepository;

  private final CurrentUserService currentUserService;

  @Override
  @CacheEvict(cacheNames = "allUsers", allEntries = true)
  public UserEntity createUser(String name, String email, String password, String accountType) {
    log.debug("Creating user with account type {}", accountType);
    UserEntity user =
        UserEntity.builder()
            .name(name)
            .email(email)
            .password(password)
            .createdAt(LocalDateTime.now())
            .updatedAt(null)
            .accountType(accountType)
            .build();
    UserEntity savedUser = userEntityRepository.save(user);
    log.info("User created successfully with userId={}", savedUser.getId());
    return savedUser;
  }

  @Override
  @Cacheable(cacheNames = "allUsers")
  public List<UserResponse> getAllUsers() {
    List<UserResponse> users =
        userEntityRepository.findAll().stream().map(UserResponse::from).toList();
    log.debug("Retrieved {} users", users.size());
    return users;
  }

  @Override
  @CacheEvict(cacheNames = "allUsers", allEntries = true)
  public UserResponse updateUser(String name, String password) {

    long userId = getCurrentUserId();
    log.debug("Updating user with userId={}", userId);

    UserEntity userEntity =
        userEntityRepository
            .findById(userId)
            .orElseThrow(
                () -> new ResourceNotFoundException("User not found for id %d".formatted(userId)));

    if (name != null && !name.isBlank()) userEntity.setName(name);

    if (password != null && !password.isBlank()) userEntity.setPassword(password);

    userEntity.setUpdatedAt(LocalDateTime.now());

    UserResponse response = UserResponse.from(userEntityRepository.save(userEntity));
    log.info("User updated successfully with userId={}", userId);
    return response;
  }

  private long getCurrentUserId() {
    return currentUserService.getCurrentUserId();
  }

  @Override
  @Cacheable(cacheNames = "nonOrgUsers", key = "#orgId")
  public List<UserResponse> getNonOrgUsers(Long orgId) {
    log.debug("Retrieving users outside organization with orgId={}", orgId);

    List<UserResponse> orgLessUsers =
        userEntityRepository.findUsersNotInOrganization(orgId).stream()
            .map(UserResponse::from)
            .toList();

    log.debug("Retrieved {} users outside organization with orgId={}", orgLessUsers.size(), orgId);
    return orgLessUsers;
  }
}
