package com.tex.cloud_task_manager;

import static org.assertj.core.api.Assertions.assertThat;

import com.tex.cloud_task_manager.User.UserEntity;
import com.tex.cloud_task_manager.User.UserEntityRepository;
import com.tex.cloud_task_manager.User.response_request.UserResponse;
import com.tex.cloud_task_manager.User.service.UserService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

class UserServiceIntegrationTest extends AbstractIntegrationTest {

  @Autowired private UserService userService;

  @Autowired private UserEntityRepository userEntityRepository;

  @BeforeEach
  void setUp() {
    userEntityRepository.deleteAll();
  }

  @Test
  void createUserShouldPersistUserToDatabase() {
    // Act
    UserEntity createdUser = userService.createUser("Kevin", "kevin@test.com", "encoded-password", "USER");

    // Assert
    assertThat(createdUser.getId()).isNotNull();
    assertThat(createdUser.getName()).isEqualTo("Kevin");
    assertThat(createdUser.getEmail()).isEqualTo("kevin@test.com");
    assertThat(createdUser.getPassword()).isEqualTo("encoded-password");
    assertThat(createdUser.getAccountType()).isEqualTo("USER");
    assertThat(createdUser.getCreatedAt()).isNotNull();
    assertThat(createdUser.getUpdatedAt()).isNull();

    Optional<UserEntity> foundUser = userEntityRepository.findById(createdUser.getId());

    assertThat(foundUser).isPresent();
    assertThat(foundUser.get().getEmail()).isEqualTo("kevin@test.com");
  }

  @Test
  void getAllUsersShouldReturnAllPersistedUsers() {
    // Arrange
    userService.createUser("Kevin", "kevin@test.com", "password-one", "USER");
    userService.createUser("Alex", "alex@test.com", "password-two", "ADMIN");

    // Act
    var users = userService.getAllUsers();

    // Assert
    assertThat(users).hasSize(2);
    assertThat(users).extracting(UserResponse::name).containsExactlyInAnyOrder("Kevin", "Alex");
  }

  @Test
  @WithMockUser(username = "kevin@test.com")
  void updateUserShouldPersistChangesForCurrentUser() {
    // Arrange
    UserEntity createdUser = userService.createUser("Kevin", "kevin@test.com", "old-password", "USER");

    // Act
    UserResponse response = userService.updateUser("Kevin Updated", "new-password");

    // Assert
    assertThat(response.name()).isEqualTo("Kevin Updated");

    UserEntity updatedUser = userEntityRepository.findById(createdUser.getId()).orElseThrow();

    assertThat(updatedUser.getName()).isEqualTo("Kevin Updated");
    assertThat(updatedUser.getPassword()).isEqualTo("new-password");
    assertThat(updatedUser.getUpdatedAt()).isNotNull();
  }
}
