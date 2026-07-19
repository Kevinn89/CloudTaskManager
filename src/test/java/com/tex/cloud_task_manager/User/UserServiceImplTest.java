package com.tex.cloud_task_manager.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tex.cloud_task_manager.Security.CurrentUserService;
import com.tex.cloud_task_manager.User.response_request.UserResponse;
import com.tex.cloud_task_manager.User.service.UserServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

  @Mock private UserEntityRepository userEntityRepository;

  @Mock private CurrentUserService currentUserService;

  @InjectMocks private UserServiceImpl userService;

  @Test
  void createUserShouldBuildUserAndSaveIt() {
    // Arrange
    String name = "Kevin";
    String email = "kevin@test.com";
    String password = "Password123!";
    String accountType = "USER";

    UserEntity savedUser =
        UserEntity.builder()
            .id(1L)
            .name(name)
            .email(email)
            .password(password)
            .accountType(accountType)
            .createdAt(LocalDateTime.now())
            .updatedAt(null)
            .build();

    when(userEntityRepository.save(any(UserEntity.class))).thenReturn(savedUser);

    // Act
    UserEntity result = userService.createUser(name, email, password, accountType);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(1L);
    assertThat(result.getName()).isEqualTo(name);
    assertThat(result.getEmail()).isEqualTo(email);
    assertThat(result.getPassword()).isEqualTo(password);
    assertThat(result.getAccountType()).isEqualTo(accountType);

    verify(userEntityRepository).save(any(UserEntity.class));
  }

  @Test
  void createUserShouldSetCreatedAtAndUpdatedAtNull() {
    // Arrange
    ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);

    when(userEntityRepository.save(any(UserEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    userService.createUser("Kevin", "kevin@test.com", "Password123!", "ADMIN");

    // Assert
    verify(userEntityRepository).save(userCaptor.capture());

    UserEntity capturedUser = userCaptor.getValue();

    assertThat(capturedUser.getCreatedAt()).isNotNull();
    assertThat(capturedUser.getUpdatedAt()).isNull();
    assertThat(capturedUser.getAccountType()).isEqualTo("ADMIN");
  }

  @Test
  void getAllUsersShouldReturnAllUsers() {
    // Arrange
    UserEntity userOne =
        UserEntity.builder()
            .id(1L)
            .name("Kevin")
            .email("kevin@test.com")
            .password("Password123!")
            .build();

    UserEntity userTwo =
        UserEntity.builder()
            .id(2L)
            .name("Alex")
            .email("alex@test.com")
            .password("Password456!")
            .build();

    when(userEntityRepository.findAll()).thenReturn(List.of(userOne, userTwo));

    // Act
    List<UserResponse> result = userService.getAllUsers();

    // Assert
    assertThat(result).hasSize(2);
    assertThat(result).extracting(UserResponse::name).containsExactly("Kevin", "Alex");

    verify(userEntityRepository).findAll();
  }

  @Test
  void getAllUsersShouldReturnEmptyListWhenNoUsersExist() {
    // Arrange
    when(userEntityRepository.findAll()).thenReturn(List.of());

    // Act
    List<UserResponse> result = userService.getAllUsers();

    // Assert
    assertThat(result).isEmpty();

    verify(userEntityRepository).findAll();
  }

  @Test
  void updateUserShouldUpdateCurrentUserAndSaveIt() {
    // Arrange
    UserEntity existingUser =
        UserEntity.builder()
            .id(1L)
            .name("Kevin")
            .email("kevin@test.com")
            .password("old-password")
            .createdAt(LocalDateTime.now())
            .updatedAt(null)
            .build();

    when(currentUserService.getCurrentUserId()).thenReturn(1L);
    when(userEntityRepository.findById(1L)).thenReturn(Optional.of(existingUser));
    when(userEntityRepository.save(existingUser)).thenReturn(existingUser);

    // Act
    UserResponse result = userService.updateUser("Kevin Updated", "new-password");

    // Assert
    assertThat(result.name()).isEqualTo("Kevin Updated");
    assertThat(existingUser.getName()).isEqualTo("Kevin Updated");
    assertThat(existingUser.getPassword()).isEqualTo("new-password");
    assertThat(existingUser.getUpdatedAt()).isNotNull();

    verify(currentUserService).getCurrentUserId();
    verify(userEntityRepository).findById(1L);
    verify(userEntityRepository).save(existingUser);
  }
}
