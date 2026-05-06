package com.tex.cloud_task_manager.RefreshToken.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.tex.cloud_task_manager.RefreshToken.RefreshTokenEntity;
import com.tex.cloud_task_manager.RefreshToken.RefreshTokenRepository;
import com.tex.cloud_task_manager.User.service.UserService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserService userService;
    
@Transactional
    @Override
    public void revokeRefreshToken(String token) {

        String tokenHash = generateHashfromToken(token);
        int refreshTokenOptional = refreshTokenRepository.revokeActiveTokensByTokenHash(tokenHash, LocalDateTime.now());
   
        if(refreshTokenOptional == 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid refresh token"
            );
        }
    }

        @Override
    public RefreshTokenEntity getRefreshTokenNotRevoked(String token) {

        String tokenHash = generateHashfromToken(token);

        Optional<RefreshTokenEntity> refreshTokenOptional = Optional.ofNullable(refreshTokenRepository.findByTokenHashAndRevoked(tokenHash, false));

        if(refreshTokenOptional.isPresent()) {

            refreshTokenOptional.get().setLastUsedAt(LocalDateTime.now());
            
            return saveRefreshTokenEntity(refreshTokenOptional.get());
        }
        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid refresh token"
        );
    }

    @Override
    public RefreshTokenEntity generateRefreshToken(String email) {  
        
    var userOptional = userService.findByEmail(email);
    String rawToken = generateSecureRandomToken();
    String tokenHash = generateHashfromToken(rawToken);

    RefreshTokenEntity refreshToken = RefreshTokenEntity.builder()
            .userId(userOptional.get().getId())
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

    return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(randomBytes);
    }

    private String generateHashfromToken (String token) {
    
        try {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        byte[] hashBytes = digest.digest(
            token.getBytes(StandardCharsets.UTF_8)
        );

        return HexFormat.of().formatHex(hashBytes);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }

    }

    private RefreshTokenEntity saveRefreshTokenEntity(RefreshTokenEntity refreshToken) {
        return refreshTokenRepository.save(refreshToken);
    }



}
