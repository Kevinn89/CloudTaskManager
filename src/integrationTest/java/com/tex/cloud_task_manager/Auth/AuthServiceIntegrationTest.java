package com.tex.cloud_task_manager.Auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tex.cloud_task_manager.AbstractIntegrationTest;
import com.tex.cloud_task_manager.Auth.response_request.AuthResponse;
import com.tex.cloud_task_manager.Auth.service.AuthService;
import com.tex.cloud_task_manager.RefreshToken.RefreshTokenEntity;
import com.tex.cloud_task_manager.RefreshToken.RefreshTokenRepository;
import com.tex.cloud_task_manager.User.UserEntity;
import com.tex.cloud_task_manager.User.UserEntityRepository;
import com.tex.cloud_task_manager.common.exception.ConflictException;
import com.tex.cloud_task_manager.common.exception.UnauthorizedException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceIntegrationTest extends AbstractIntegrationTest {

  @Autowired private AuthService authService;

  @Autowired private UserEntityRepository userEntityRepository;

  @Autowired private RefreshTokenRepository refreshTokenRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @BeforeEach
  void setUp() {
    refreshTokenRepository.deleteAll();
    userEntityRepository.deleteAll();
  }

  @Test
  void registerUserShouldCreateUserWithEncodedPasswordWhenEmailDoesNotExist() {
    // Act
    AuthResponse response =
        (AuthResponse) authService.registerUser("Kevin", "kevin@test.com", "Password123!");

    // Assert response
    assertThat(response.message()).isEqualTo("User registered successfully ");
    assertThat(response.token()).isNull();
    assertThat(response.tokenExpiration()).isNull();
    assertThat(response.refreshToken()).isNull();
    assertThat(response.refreshTokenExpiration()).isNull();

    // Assert database
    UserEntity savedUser = userEntityRepository.findByEmail("kevin@test.com").orElseThrow();

    assertThat(savedUser.getId()).isNotNull();
    assertThat(savedUser.getName()).isEqualTo("Kevin");
    assertThat(savedUser.getEmail()).isEqualTo("kevin@test.com");
    assertThat(savedUser.getCreatedAt()).isNotNull();

    assertThat(savedUser.getPassword()).isNotEqualTo("Password123!");
    assertThat(passwordEncoder.matches("Password123!", savedUser.getPassword())).isTrue();
  }

  @Test
  void registerUserShouldNotCreateUserWhenEmailAlreadyExists() {

    authService.registerUser("Kevin", "kevin@test.com", "Password123!");

    assertThatThrownBy(
            () -> authService.registerUser("Kevin Again", "kevin@test.com", "AnotherPassword123!"))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("Email is already in use");

    UserEntity savedUser = userEntityRepository.findByEmail("kevin@test.com").orElseThrow();

    assertThat(savedUser.getId()).isNotNull();
    assertThat(savedUser.getEmail()).isEqualTo("kevin@test.com");
    assertThat(savedUser.getCreatedAt()).isNotNull();

    assertThat(savedUser.getName()).isNotEqualTo("Kevin Again");
    assertThat(savedUser.getPassword()).isNotEqualTo("Password123!");
    assertThat(passwordEncoder.matches("Password123!", savedUser.getPassword())).isTrue();
  }

  @Test
  void loginUserShouldReturnJwtAndRefreshTokenWhenCredentialsAreValid() {
    // Arrange
    authService.registerUser("Kevin", "kevin@test.com", "Password123!");

    // Act
    AuthResponse response = (AuthResponse) authService.loginUser("kevin@test.com", "Password123!");

    // Assert response
    assertThat(response.message()).isEqualTo("User logged in successfully");
    assertThat(response.token()).isNotBlank();
    assertThat(response.tokenExpiration()).isNotNull();
    assertThat(response.refreshToken()).isNotBlank();
    assertThat(response.refreshTokenExpiration()).isNotBlank();

    // Assert refresh token was persisted
    String tokenHash = sha256(response.refreshToken());

    RefreshTokenEntity savedRefreshToken =
        refreshTokenRepository.findByTokenHashAndRevoked(tokenHash, false);

    assertThat(savedRefreshToken).isNotNull();
    assertThat(savedRefreshToken.getRawToken()).isNullOrEmpty();
    assertThat(savedRefreshToken.getTokenHash()).isEqualTo(tokenHash);
    assertThat(savedRefreshToken.isRevoked()).isFalse();
    assertThat(savedRefreshToken.getExpiresAt()).isNotNull();
    assertThat(savedRefreshToken.getCreatedAt()).isNotNull();
  }

  @Test
  void loginUserShouldReturnInvalidCredentialsWhenPasswordIsWrong() {
    // Arrange
    authService.registerUser("Kevin", "kevin@test.com", "Password123!");

    assertThatThrownBy(() -> authService.loginUser("kevin@test.com", "WrongPassword123!"))
        .isInstanceOf(UnauthorizedException.class)
        .hasMessageContaining("Invalid credentials");

    UserEntity savedUser = userEntityRepository.findByEmail("kevin@test.com").orElseThrow();

    assertThat(savedUser.getId()).isNotNull();
    assertThat(savedUser.getEmail()).isEqualTo("kevin@test.com");
    assertThat(savedUser.getCreatedAt()).isNotNull();

    assertThat(savedUser.getPassword()).isNotEqualTo("Password123!");
    assertThat(passwordEncoder.matches("Password123!", savedUser.getPassword())).isTrue();
  }

  @Test
  void loginUserShouldReturnInvalidCredentialsWhenUserDoesNotExist() {

    assertThatThrownBy(() -> authService.loginUser("kevin@test.com", "WrongPassword123!"))
        .isInstanceOf(UnauthorizedException.class)
        .hasMessageContaining("Invalid credentials");

    var savedUser = userEntityRepository.findByEmail("kevin@test.com");

    assertTrue(savedUser.isEmpty());
  }

  @Test
  void refreshShouldReturnNewJwtWhenRefreshTokenIsValid() {
    // Arrange
    authService.registerUser("Kevin", "kevin@test.com", "Password123!");

    AuthResponse loginResponse = authService.loginUser("kevin@test.com", "Password123!");

    String refreshToken = loginResponse.refreshToken();

    // Act
    AuthResponse refreshResponse = authService.refresh(refreshToken, "kevin@test.com");

    // Assert
    assertThat(refreshResponse.message()).isEqualTo("Token refreshed successfully");
    assertThat(refreshResponse.token()).isNotBlank();
    assertThat(refreshResponse.tokenExpiration()).isNotNull();
    assertThat(refreshResponse.refreshToken()).isEqualTo(refreshToken);
    assertThat(refreshResponse.refreshTokenExpiration()).isNotBlank();

    RefreshTokenEntity savedRefreshToken =
        refreshTokenRepository.findByTokenHashAndRevoked(sha256(refreshToken), false);

    assertThat(savedRefreshToken).isNotNull();
  }

  @Test
  void logoutShouldRevokeRefreshToken() {
    // Arrange
    authService.registerUser("Kevin", "kevin@test.com", "Password123!");

    AuthResponse loginResponse = authService.loginUser("kevin@test.com", "Password123!");

    String refreshToken = loginResponse.refreshToken();

    // Act
    AuthResponse logoutResponse = authService.logout(refreshToken);

    // Assert response
    assertThat(logoutResponse).isInstanceOf(AuthResponse.class);

    AuthResponse authResponse = logoutResponse;

    assertThat(authResponse.message()).isEqualTo("User logged out successfully");
    assertThat(authResponse.token()).isNull();
    assertThat(authResponse.tokenExpiration()).isNull();
    assertThat(authResponse.refreshToken()).isNull();
    assertThat(authResponse.refreshTokenExpiration()).isNull();

    // Assert database
    RefreshTokenEntity revokedToken =
        refreshTokenRepository.findByTokenHashAndRevoked(sha256(refreshToken), true);

    assertThat(revokedToken).isNotNull();
    assertThat(revokedToken.isRevoked()).isTrue();
    assertThat(revokedToken.getRevokedAt()).isNotNull();
  }

  @Test
  void refreshShouldThrowUnauthorizedAfterLogout() {
    // Arrange
    authService.registerUser("Kevin", "kevin@test.com", "Password123!");

    AuthResponse loginResponse =
        (AuthResponse) authService.loginUser("kevin@test.com", "Password123!");

    String refreshToken = loginResponse.refreshToken();

    authService.logout(refreshToken);

    // Act + Assert
    assertThatThrownBy(() -> authService.refresh(refreshToken, "kevin@test.com"))
        .isInstanceOf(UnauthorizedException.class)
        .hasMessageContaining("Invalid refresh token");
  }

  @Test
  void logoutShouldThrowUnauthorizedWhenRefreshTokenDoesNotExist() {
    assertThatThrownBy(() -> authService.logout("missing-refresh-token"))
        .isInstanceOf(UnauthorizedException.class)
        .hasMessageContaining("Invalid refresh token");
  }

  private static String sha256(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");

      byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));

      return HexFormat.of().formatHex(hashBytes);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
