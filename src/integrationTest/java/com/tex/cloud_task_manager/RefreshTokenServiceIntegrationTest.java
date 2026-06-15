package com.tex.cloud_task_manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tex.cloud_task_manager.RefreshToken.RefreshTokenEntity;
import com.tex.cloud_task_manager.RefreshToken.RefreshTokenRepository;
import com.tex.cloud_task_manager.RefreshToken.service.RefreshTokenService;
import com.tex.cloud_task_manager.User.UserEntity;
import com.tex.cloud_task_manager.User.UserEntityRepository;
import com.tex.cloud_task_manager.User.service.UserService;
import com.tex.cloud_task_manager.common.exception.UnauthorizedException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RefreshTokenServiceIntegrationTest extends AbstractIntegrationTest {

  @Autowired private RefreshTokenService refreshTokenService;

  @Autowired private RefreshTokenRepository refreshTokenRepository;

  @Autowired private UserService userService;

  @Autowired private UserEntityRepository userEntityRepository;

  @BeforeEach
  void setUp() {
    refreshTokenRepository.deleteAll();
    userEntityRepository.deleteAll();
  }

  @Test
  void generateRefreshTokenShouldPersistRefreshTokenWhenUserExists() {
    // Arrange
    UserEntity user = userService.createUser("Kevin", "kevin@test.com", "encoded-password", "USER");

    // Act
    RefreshTokenEntity refreshToken = refreshTokenService.generateRefreshToken("kevin@test.com");

    // Assert
    assertThat(refreshToken.getId()).isNotNull();
    assertThat(refreshToken.getUserId()).isEqualTo(user.getId());
    assertThat(refreshToken.getRawToken()).isNotBlank();
    assertThat(refreshToken.getTokenHash()).isNotBlank();
    assertThat(refreshToken.getTokenHash()).isEqualTo(sha256(refreshToken.getRawToken()));

    assertThat(refreshToken.getExpiresAt()).isAfter(LocalDateTime.now());
    assertThat(refreshToken.getCreatedAt()).isNotNull();
    assertThat(refreshToken.getLastUsedAt()).isNull();
    assertThat(refreshToken.getRevokedAt()).isNull();
    assertThat(refreshToken.isRevoked()).isFalse();

    RefreshTokenEntity foundToken =
        refreshTokenRepository.findById(refreshToken.getId()).orElseThrow();

    assertThat(foundToken.getUserId()).isEqualTo(user.getId());
    assertThat(foundToken.getTokenHash()).isEqualTo(refreshToken.getTokenHash());
    assertThat(foundToken.isRevoked()).isFalse();
  }

  @Test
  void generateRefreshTokenShouldThrowWhenUserDoesNotExist() {

    String email = "missing@test.com";
    assertThatThrownBy(() -> refreshTokenService.generateRefreshToken(email))
        .isInstanceOf(UnauthorizedException.class)
        .hasMessageContaining("User not found for this email %s".formatted(email));

    assertThat(refreshTokenRepository.findAll()).isEmpty();
  }

  @Test
  void getRefreshTokenNotRevokedShouldThrowWhenTokenIsMissing() {
    assertThatThrownBy(() -> refreshTokenService.getRefreshTokenNotRevoked("bad-refresh-token"))
        .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  void revokeRefreshTokenShouldMarkTokenAsRevoked() {
    // Arrange
    userService.createUser("Kevin", "kevin@test.com", "encoded-password", "USER");

    RefreshTokenEntity generatedToken = refreshTokenService.generateRefreshToken("kevin@test.com");

    String rawToken = generatedToken.getRawToken();

    // Act
    refreshTokenService.revokeRefreshToken(rawToken);

    // Assert
    RefreshTokenEntity foundToken =
        refreshTokenRepository.findById(generatedToken.getId()).orElseThrow();

    assertThat(foundToken.isRevoked()).isTrue();
    assertThat(foundToken.getRevokedAt()).isNotNull();
  }

  @Test
  void revokeRefreshTokenShouldThrowUnauthorizedWhenTokenDoesNotExist() {
    assertThatThrownBy(() -> refreshTokenService.revokeRefreshToken("missing-refresh-token"))
        .isInstanceOf(UnauthorizedException.class)
        .hasMessageContaining("Invalid refresh token");
  }

  @Test
  void getRefreshTokenNotRevokedShouldThrowAfterTokenIsRevoked() {
    // Arrange
    userService.createUser("Kevin", "kevin@test.com", "encoded-password", "USER");

    RefreshTokenEntity generatedToken = refreshTokenService.generateRefreshToken("kevin@test.com");

    String rawToken = generatedToken.getRawToken();

    refreshTokenService.revokeRefreshToken(rawToken);

    // Act + Assert
    assertThatThrownBy(() -> refreshTokenService.getRefreshTokenNotRevoked(rawToken))
        .isInstanceOf(UnauthorizedException.class);
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
