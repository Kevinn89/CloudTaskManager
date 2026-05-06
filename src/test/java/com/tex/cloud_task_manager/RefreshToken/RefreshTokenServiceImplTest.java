package com.tex.cloud_task_manager.RefreshToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.tex.cloud_task_manager.RefreshToken.service.RefreshTokenServiceImpl;
import com.tex.cloud_task_manager.User.UserEntity;
import com.tex.cloud_task_manager.User.service.UserService;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    @Test
    void revokeRefreshTokenShouldRevokeTokenWhenTokenExists() {
        // Arrange
        String rawToken = "raw-refresh-token";
        String tokenHash = sha256(rawToken);

        when(refreshTokenRepository.revokeActiveTokensByTokenHash(
                eq(tokenHash),
                any(LocalDateTime.class)
        )).thenReturn(1);

        // Act
        refreshTokenService.revokeRefreshToken(rawToken);

        // Assert
        verify(refreshTokenRepository).revokeActiveTokensByTokenHash(
                eq(tokenHash),
                any(LocalDateTime.class)
        );
    }

    @Test
    void revokeRefreshTokenShouldThrowUnauthorizedWhenTokenDoesNotExist() {
        // Arrange
        String rawToken = "bad-refresh-token";
        String tokenHash = sha256(rawToken);

        when(refreshTokenRepository.revokeActiveTokensByTokenHash(
                eq(tokenHash),
                any(LocalDateTime.class)
        )).thenReturn(0);

        // Act + Assert
        assertThatThrownBy(() -> refreshTokenService.revokeRefreshToken(rawToken))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401 UNAUTHORIZED")
                .hasMessageContaining("Invalid refresh token");

        verify(refreshTokenRepository).revokeActiveTokensByTokenHash(
                eq(tokenHash),
                any(LocalDateTime.class)
        );
    }

    @Test
    void getRefreshTokenNotRevokedShouldUpdateLastUsedAtAndSaveTokenWhenTokenExists() {
        // Arrange
        String rawToken = "raw-refresh-token";
        String tokenHash = sha256(rawToken);

        RefreshTokenEntity existingRefreshToken = RefreshTokenEntity.builder()
                .id(1L)
                .userId(10L)
                .tokenHash(tokenHash)
                .rawToken(rawToken)
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now().minusDays(1))
                .lastUsedAt(null)
                .revokedAt(null)
                .build();

        when(refreshTokenRepository.findByTokenHashAndRevoked(tokenHash, false))
                .thenReturn(existingRefreshToken);

        when(refreshTokenRepository.save(any(RefreshTokenEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        RefreshTokenEntity result = refreshTokenService.getRefreshTokenNotRevoked(rawToken);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTokenHash()).isEqualTo(tokenHash);
        assertThat(result.isRevoked()).isFalse();
        assertThat(result.getLastUsedAt()).isNotNull();

        verify(refreshTokenRepository).findByTokenHashAndRevoked(tokenHash, false);
        verify(refreshTokenRepository).save(existingRefreshToken);
    }

    @Test
    void getRefreshTokenNotRevokedShouldSaveTokenWithUpdatedLastUsedAt() {
        // Arrange
        String rawToken = "raw-refresh-token";
        String tokenHash = sha256(rawToken);

        RefreshTokenEntity existingRefreshToken = RefreshTokenEntity.builder()
                .id(1L)
                .userId(10L)
                .tokenHash(tokenHash)
                .rawToken(rawToken)
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now().minusDays(1))
                .lastUsedAt(null)
                .revokedAt(null)
                .build();

        ArgumentCaptor<RefreshTokenEntity> refreshTokenCaptor =
                ArgumentCaptor.forClass(RefreshTokenEntity.class);

        when(refreshTokenRepository.findByTokenHashAndRevoked(tokenHash, false))
                .thenReturn(existingRefreshToken);

        when(refreshTokenRepository.save(any(RefreshTokenEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        refreshTokenService.getRefreshTokenNotRevoked(rawToken);

        // Assert
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());

        RefreshTokenEntity capturedRefreshToken = refreshTokenCaptor.getValue();

        assertThat(capturedRefreshToken.getId()).isEqualTo(1L);
        assertThat(capturedRefreshToken.getTokenHash()).isEqualTo(tokenHash);
        assertThat(capturedRefreshToken.getLastUsedAt()).isNotNull();
    }

    @Test
    void getRefreshTokenNotRevokedShouldThrowWhenTokenDoesNotExist() {
        // Arrange
        String rawToken = "missing-refresh-token";
        String tokenHash = sha256(rawToken);

        when(refreshTokenRepository.findByTokenHashAndRevoked(tokenHash, false))
                .thenReturn(null);

        // Act + Assert
        assertThatThrownBy(() -> refreshTokenService.getRefreshTokenNotRevoked(rawToken))
                .isInstanceOf(ResponseStatusException.class);

        verify(refreshTokenRepository).findByTokenHashAndRevoked(tokenHash, false);
        verify(refreshTokenRepository, never()).save(any(RefreshTokenEntity.class));
    }

    @Test
    void generateRefreshTokenShouldCreateAndSaveRefreshTokenWhenUserExists() {
        // Arrange
        String email = "test@example.com";

        UserEntity user = UserEntity.builder()
                .id(10L)
                .name("Kevin")
                .email(email)
                .password("encoded-password")
                .build();

        when(userService.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(refreshTokenRepository.save(any(RefreshTokenEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        RefreshTokenEntity result = refreshTokenService.generateRefreshToken(email);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(10L);
        assertThat(result.getTokenHash()).isNotBlank();
        assertThat(result.getRawToken()).isNotBlank();
        assertThat(result.getExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getLastUsedAt()).isNull();
        assertThat(result.getRevokedAt()).isNull();
        assertThat(result.isRevoked()).isFalse();


        verify(userService).findByEmail(email);
        verify(refreshTokenRepository, times(1)).save(any(RefreshTokenEntity.class));
    }

    @Test
    void generateRefreshTokenShouldSaveRefreshTokenWithCorrectFields() {
        // Arrange
        String email = "test@example.com";

        UserEntity user = UserEntity.builder()
                .id(10L)
                .name("Kevin")
                .email(email)
                .password("encoded-password")
                .build();

        ArgumentCaptor<RefreshTokenEntity> refreshTokenCaptor =
                ArgumentCaptor.forClass(RefreshTokenEntity.class);

        when(userService.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(refreshTokenRepository.save(any(RefreshTokenEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        refreshTokenService.generateRefreshToken(email);

        // Assert
        verify(refreshTokenRepository, times(1)).save(refreshTokenCaptor.capture());

        RefreshTokenEntity capturedRefreshToken = refreshTokenCaptor.getAllValues().get(0);

        assertThat(capturedRefreshToken.getUserId()).isEqualTo(10L);
        assertThat(capturedRefreshToken.getRawToken()).isNotBlank();
        assertThat(capturedRefreshToken.getTokenHash()).isEqualTo(
                sha256(capturedRefreshToken.getRawToken())
        );
        assertThat(capturedRefreshToken.getExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(capturedRefreshToken.getCreatedAt()).isNotNull();
        assertThat(capturedRefreshToken.getLastUsedAt()).isNull();
        assertThat(capturedRefreshToken.getRevokedAt()).isNull();
        assertThat(capturedRefreshToken.isRevoked()).isFalse();

    }

    @Test
    void generateRefreshTokenShouldThrowWhenUserDoesNotExist() {
        // Arrange
        String email = "missing@example.com";

        when(userService.findByEmail(email))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> refreshTokenService.generateRefreshToken(email))
                .isInstanceOf(java.util.NoSuchElementException.class);

        verify(userService).findByEmail(email);
        verify(refreshTokenRepository, never()).save(any(RefreshTokenEntity.class));
    }

    private static String sha256(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}