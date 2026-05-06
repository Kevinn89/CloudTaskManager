package com.tex.cloud_task_manager.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tex.cloud_task_manager.User.service.UserServiceImpl;

import lombok.RequiredArgsConstructor;

@ExtendWith(MockitoExtension.class)
@RequiredArgsConstructor
class UserServiceImplTest {

    @Mock
    private UserEntityRepository userEntityRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void createUserShouldBuildUserAndSaveIt() {
        // Arrange
        String name = "Kevin";
        String email = "kevin@test.com";
        String password = "Password123!";

        UserEntity savedUser = UserEntity.builder()
                .id(1L)
                .name(name)
                .email(email)
                .password(password)
                .createdAt(LocalDateTime.now())
                .updatedAt(null)
                .build();

        when(userEntityRepository.save(any(UserEntity.class)))
                .thenReturn(savedUser);

        // Act
        UserEntity result = userService.createUser(name, email, password);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo(name);
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getPassword()).isEqualTo(password);

        verify(userEntityRepository).save(any(UserEntity.class));
    }

    @Test
    void createUserShouldSetCreatedAtAndUpdatedAtNull() {
        // Arrange
        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);

        when(userEntityRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        userService.createUser("Kevin", "kevin@test.com", "Password123!");

        // Assert
        verify(userEntityRepository).save(userCaptor.capture());

        UserEntity capturedUser = userCaptor.getValue();

        assertThat(capturedUser.getCreatedAt()).isNotNull();
        assertThat(capturedUser.getUpdatedAt()).isNull();
    }

    @Test
    void getUserByIdShouldReturnUserWhenUserExists() {
        // Arrange
        UserEntity user = UserEntity.builder()
                .id(1L)
                .name("Kevin")
                .email("kevin@test.com")
                .password("Password123!")
                .build();

        when(userEntityRepository.findById(1L))
                .thenReturn(Optional.of(user));

        // Act
        Optional<UserEntity> result = userService.getUserById(1L);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getEmail()).isEqualTo("kevin@test.com");

        verify(userEntityRepository).findById(1L);
    }

    @Test
    void getUserByIdShouldReturnEmptyWhenUserDoesNotExist() {
        // Arrange
        when(userEntityRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act
        Optional<UserEntity> result = userService.getUserById(999L);

        // Assert
        assertThat(result).isEmpty();

        verify(userEntityRepository).findById(999L);
    }

    @Test
    void deleteUserShouldDeleteUserById() {
        // Act
        userService.deleteUser(1L);

        // Assert
        verify(userEntityRepository).deleteById(1L);
    }

    @Test
    void findByEmailShouldReturnUserWhenEmailExists() {
        // Arrange
        UserEntity user = UserEntity.builder()
                .id(1L)
                .name("Kevin")
                .email("kevin@test.com")
                .password("Password123!")
                .build();

        when(userEntityRepository.findByEmail("kevin@test.com"))
                .thenReturn(Optional.of(user));

        // Act
        Optional<UserEntity> result = userService.findByEmail("kevin@test.com");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("kevin@test.com");

        verify(userEntityRepository).findByEmail("kevin@test.com");
    }

    @Test
    void findByEmailShouldReturnEmptyWhenEmailDoesNotExist() {
        // Arrange
        when(userEntityRepository.findByEmail("missing@test.com"))
                .thenReturn(Optional.empty());

        // Act
        Optional<UserEntity> result = userService.findByEmail("missing@test.com");

        // Assert
        assertThat(result).isEmpty();

        verify(userEntityRepository).findByEmail("missing@test.com");
    }

    @Test
    void getAllUsersShouldReturnAllUsers() {
        // Arrange
        UserEntity userOne = UserEntity.builder()
                .id(1L)
                .name("Kevin")
                .email("kevin@test.com")
                .password("Password123!")
                .build();

        UserEntity userTwo = UserEntity.builder()
                .id(2L)
                .name("Alex")
                .email("alex@test.com")
                .password("Password456!")
                .build();

        when(userEntityRepository.findAll())
                .thenReturn(List.of(userOne, userTwo));

        // Act
        List<UserEntity> result = userService.getAllUsers();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(UserEntity::getEmail)
                .containsExactly("kevin@test.com", "alex@test.com");

        verify(userEntityRepository).findAll();
    }

    @Test
    void getAllUsersShouldReturnEmptyListWhenNoUsersExist() {
        // Arrange
        when(userEntityRepository.findAll())
                .thenReturn(List.of());

        // Act
        List<UserEntity> result = userService.getAllUsers();

        // Assert
        assertThat(result).isEmpty();

        verify(userEntityRepository).findAll();
    }
}
