package com.tex.cloud_task_manager;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.tex.cloud_task_manager.User.UserEntity;
import com.tex.cloud_task_manager.User.UserEntityRepository;
import com.tex.cloud_task_manager.User.service.UserService;


class UserServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserEntityRepository userEntityRepository;

    @BeforeEach
    void setUp() {
        userEntityRepository.deleteAll();
    }

    @Test
    void createUserShouldPersistUserToDatabase() {
        // Act
        UserEntity createdUser = userService.createUser(
                "Kevin",
                "kevin@test.com",
                "encoded-password"
        );

        // Assert
        assertThat(createdUser.getId()).isNotNull();
        assertThat(createdUser.getName()).isEqualTo("Kevin");
        assertThat(createdUser.getEmail()).isEqualTo("kevin@test.com");
        assertThat(createdUser.getPassword()).isEqualTo("encoded-password");
        assertThat(createdUser.getCreatedAt()).isNotNull();
        assertThat(createdUser.getUpdatedAt()).isNull();

        Optional<UserEntity> foundUser = userEntityRepository.findById(createdUser.getId());

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("kevin@test.com");
    }

    @Test
    void getUserByIdShouldReturnUserWhenUserExists() {
        // Arrange
        UserEntity savedUser = userService.createUser(
                "Kevin",
                "kevin@test.com",
                "encoded-password"
        );

        // Act
        Optional<UserEntity> result = userService.getUserById(savedUser.getId());

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(savedUser.getId());
        assertThat(result.get().getEmail()).isEqualTo("kevin@test.com");
    }

    @Test
    void getUserByIdShouldReturnEmptyWhenUserDoesNotExist() {
        // Act
        Optional<UserEntity> result = userService.getUserById(999L);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void findByEmailShouldReturnUserWhenEmailExists() {
        // Arrange
        userService.createUser(
                "Kevin",
                "kevin@test.com",
                "encoded-password"
        );

        // Act
        Optional<UserEntity> result = userService.findByEmail("kevin@test.com");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Kevin");
        assertThat(result.get().getEmail()).isEqualTo("kevin@test.com");
    }

    @Test
    void findByEmailShouldReturnEmptyWhenEmailDoesNotExist() {
        // Act
        Optional<UserEntity> result = userService.findByEmail("missing@test.com");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void getAllUsersShouldReturnAllPersistedUsers() {
        // Arrange
        userService.createUser("Kevin", "kevin@test.com", "password-one");
        userService.createUser("Alex", "alex@test.com", "password-two");

        // Act
        var users = userService.getAllUsers();

        // Assert
        assertThat(users).hasSize(2);
        assertThat(users)
                .extracting(UserEntity::getEmail)
                .containsExactlyInAnyOrder("kevin@test.com", "alex@test.com");
    }

    @Test
    void deleteUserShouldRemoveUserFromDatabase() {
        // Arrange
        UserEntity savedUser = userService.createUser(
                "Kevin",
                "kevin@test.com",
                "encoded-password"
        );

        // Act
        userService.deleteUser(savedUser.getId());

        // Assert
        Optional<UserEntity> result = userEntityRepository.findById(savedUser.getId());

        assertThat(result).isEmpty();
    }
}
