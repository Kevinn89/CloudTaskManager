package com.tex.cloud_task_manager.Security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import com.tex.cloud_task_manager.Config.JwtProperties;

import io.jsonwebtoken.JwtException;

class JwtServiceTest {

    private JwtService jwtService;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        String rawSecret = "test-secret-key-test-secret-key-test-secret-key-test-secret-key";
        String base64Secret = Base64.getEncoder().encodeToString(rawSecret.getBytes());

        JwtProperties jwtProperties = new JwtProperties(
                base64Secret,
                15L,
                7L
        );

        jwtService = new JwtService(jwtProperties);

        userDetails = org.springframework.security.core.userdetails.User
                .withUsername("kevin@test.com")
                .password("encoded-password")
                .roles("USER")
                .build();
    }

    @Test
    void generateTokenShouldCreateToken() {
        // Act
        String token = jwtService.generateToken(userDetails);

        // Assert
        assertThat(token).isNotBlank();
    }

    @Test
    void generateTokenShouldUseUsernameAsSubject() {
        // Act
        String token = jwtService.generateToken(userDetails);

        // Assert
        assertThat(jwtService.extractUsername(token)).isEqualTo("kevin@test.com");
    }

    @Test
    void generateTokenWithEmailShouldUseEmailAsSubject() {
        // Act
        String token = jwtService.generateToken("kevin@test.com");

        // Assert
        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("kevin@test.com");
    }

    @Test
    void isTokenValidShouldReturnTrueWhenUsernameMatchesAndTokenIsNotExpired() {
        // Arrange
        String token = jwtService.generateToken(userDetails);

        // Act
        boolean result = jwtService.isTokenValid(token, userDetails);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void isTokenValidShouldReturnFalseWhenUsernameDoesNotMatch() {
        // Arrange
        String token = jwtService.generateToken(userDetails);

        UserDetails differentUser = org.springframework.security.core.userdetails.User
                .withUsername("other@test.com")
                .password("encoded-password")
                .roles("USER")
                .build();

        // Act
        boolean result = jwtService.isTokenValid(token, differentUser);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void extractExpirationShouldReturnExpirationDateString() {
        // Arrange
        String token = jwtService.generateToken(userDetails);

        // Act
        String expiration = jwtService.extractExpiration(token);

        // Assert
        assertThat(expiration).isNotBlank();
    }

    @Test
    void extractUsernameShouldThrowWhenTokenWasSignedWithDifferentSecret() {
        // Arrange
        String token = jwtService.generateToken(userDetails);

        String otherRawSecret = "different-secret-key-different-secret-key-different-secret-key";
        String otherBase64Secret = Base64.getEncoder().encodeToString(otherRawSecret.getBytes());

        JwtProperties otherJwtProperties = new JwtProperties(
                otherBase64Secret,
                15L,
                7L
        );

        JwtService otherJwtService = new JwtService(otherJwtProperties);

        // Act + Assert
        assertThatThrownBy(() -> otherJwtService.extractUsername(token))
                .isInstanceOf(JwtException.class);
    }
}
