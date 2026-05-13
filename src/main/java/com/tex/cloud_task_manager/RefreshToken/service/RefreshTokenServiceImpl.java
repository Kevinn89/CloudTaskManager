package com.tex.cloud_task_manager.RefreshToken.service;

import com.tex.cloud_task_manager.RefreshToken.RefreshTokenEntity;
import com.tex.cloud_task_manager.RefreshToken.RefreshTokenRepository;
import com.tex.cloud_task_manager.User.UserEntityRepository;
import com.tex.cloud_task_manager.common.exception.UnauthorizedException;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

  private final RefreshTokenRepository refreshTokenRepository;

  private final UserEntityRepository userEntityRepository;

  @Transactional
  @Override
  public void revokeRefreshToken(String token) {

    String tokenHash = generateHashfromToken(token);

    int rowsUpdated =
        refreshTokenRepository.revokeActiveTokensByTokenHash(tokenHash, LocalDateTime.now());
    if (rowsUpdated == 0) {
      throw new UnauthorizedException("Invalid refresh token");
    }
  }

  @Override
  public RefreshTokenEntity getRefreshTokenNotRevoked(String token) {

    String tokenHash = generateHashfromToken(token);

    var refreshTokenOptional =
        Optional.ofNullable(refreshTokenRepository.findByTokenHashAndRevoked(tokenHash, false))
            .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

    return saveRefreshTokenEntity(refreshTokenOptional);
  }

  @Override
  public RefreshTokenEntity generateRefreshToken(String email) {

    var userOptional =
        userEntityRepository
            .findByEmail(email)
            .orElseThrow(
                () ->
                    new UnauthorizedException("User not found for this email %s".formatted(email)));

    String rawToken = generateSecureRandomToken();

    String tokenHash = generateHashfromToken(rawToken);

    RefreshTokenEntity refreshToken =
        RefreshTokenEntity.builder()
            .userId(userOptional.getId())
            .expiresAt(LocalDateTime.now().plusDays(7))
            .createdAt(LocalDateTime.now())
            .lastUsedAt(null)
            .revokedAt(null)
            .tokenHash(tokenHash)
            .rawToken(rawToken)
            .revoked(false)
            .build();

    return saveRefreshTokenEntity(refreshToken);
  }

  private String generateSecureRandomToken() {

    byte[] randomBytes = new byte[64];
    new SecureRandom().nextBytes(randomBytes);

    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
  }

  private String generateHashfromToken(String token) {

    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");

      byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));

      return HexFormat.of().formatHex(hashBytes);

    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }

  private RefreshTokenEntity saveRefreshTokenEntity(RefreshTokenEntity refreshToken) {
    return refreshTokenRepository.save(refreshToken);
  }
}
