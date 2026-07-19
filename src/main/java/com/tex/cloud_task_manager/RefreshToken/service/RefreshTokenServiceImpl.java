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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

  private final RefreshTokenRepository refreshTokenRepository;

  private final UserEntityRepository userEntityRepository;

  @Transactional
  @Override
  public void revokeRefreshToken(String token) {
    log.debug("Revoking refresh token");

    String tokenHash = generateHashfromToken(token);

    int rowsUpdated =
        refreshTokenRepository.revokeActiveTokensByTokenHash(tokenHash, LocalDateTime.now());
    if (rowsUpdated == 0) {
      log.warn("Refresh token revocation rejected because no active token matched");
      throw new UnauthorizedException("Invalid refresh token");
    }
    log.info("Refresh token revoked successfully");
  }

  @Override
  public RefreshTokenEntity getRefreshTokenNotRevoked(String token) {
    log.debug("Retrieving active refresh token");

    String tokenHash = generateHashfromToken(token);

    var refreshTokenOptional =
        Optional.ofNullable(refreshTokenRepository.findByTokenHashAndRevoked(tokenHash, false))
            .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

    RefreshTokenEntity refreshToken = saveRefreshTokenEntity(refreshTokenOptional);
    log.debug("Active refresh token retrieved for userId={}", refreshToken.getUserId());
    return refreshToken;
  }

  @Override
  public RefreshTokenEntity generateRefreshToken(String email) {
    log.debug("Generating refresh token");

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

    RefreshTokenEntity savedToken = saveRefreshTokenEntity(refreshToken);
    log.info("Refresh token generated successfully for userId={}", userOptional.getId());
    return savedToken;
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
