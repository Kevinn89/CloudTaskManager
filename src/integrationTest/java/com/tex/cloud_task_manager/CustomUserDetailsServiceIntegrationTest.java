package com.tex.cloud_task_manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tex.cloud_task_manager.Security.CustomUserDetailsService;
import com.tex.cloud_task_manager.User.UserEntity;
import com.tex.cloud_task_manager.User.UserEntityRepository;
import com.tex.cloud_task_manager.User.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class CustomUserDetailsServiceIntegrationTest extends AbstractIntegrationTest {

  @Autowired private CustomUserDetailsService customUserDetailsService;

  @Autowired private UserService userService;

  @Autowired private UserEntityRepository userEntityRepository;

  @BeforeEach
  void setUp() {
    userEntityRepository.deleteAll();
  }

  @Test
  void loadUserByUsernameShouldReturnUserDetailsWhenUserExists() {
    // Arrange
    UserEntity savedUser = userService.createUser("Kevin", "kevin@test.com", "encoded-password");

    // Act
    UserDetails userDetails = customUserDetailsService.loadUserByUsername("kevin@test.com");

    // Assert
    assertThat(userDetails).isNotNull();
    assertThat(userDetails.getUsername()).isEqualTo(savedUser.getEmail());
    assertThat(userDetails.getPassword()).isEqualTo(savedUser.getPassword());

    assertThat(userDetails.getAuthorities())
        .extracting(authority -> authority.getAuthority())
        .containsExactly("ROLE_USER");

    assertThat(userDetails.isAccountNonExpired()).isTrue();
    assertThat(userDetails.isAccountNonLocked()).isTrue();
    assertThat(userDetails.isCredentialsNonExpired()).isTrue();
    assertThat(userDetails.isEnabled()).isTrue();
  }

  @Test
  void loadUserByUsernameShouldThrowUsernameNotFoundExceptionWhenUserDoesNotExist() {
    // Act + Assert
    assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("missing@test.com"))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessageContaining("User not found");
  }
}
