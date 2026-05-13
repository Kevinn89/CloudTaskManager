package com.tex.cloud_task_manager.Auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tex.cloud_task_manager.Auth.response_request.AuthResponse;
import com.tex.cloud_task_manager.Auth.service.AuthServiceImpl;
import com.tex.cloud_task_manager.RefreshToken.RefreshTokenEntity;
import com.tex.cloud_task_manager.RefreshToken.service.RefreshTokenService;
import com.tex.cloud_task_manager.Security.CustomUserDetailsService;
import com.tex.cloud_task_manager.Security.JwtService;
import com.tex.cloud_task_manager.User.UserEntity;
import com.tex.cloud_task_manager.User.UserEntityRepository;
import com.tex.cloud_task_manager.User.service.UserService;
import com.tex.cloud_task_manager.common.exception.ConflictException;
import com.tex.cloud_task_manager.common.exception.UnauthorizedException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserService userService;

  @Mock private UserEntityRepository userEntityRepository;

  @Mock private CustomUserDetailsService customUserDetailsService;

  @Mock private AuthenticationManager authenticationManager;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private JwtService jwtService;

  @Mock private RefreshTokenService refreshTokenService;

  @Mock private UserDetails userDetails;

  @InjectMocks private AuthServiceImpl authService;

  private UserEntity userEntity;

  private RefreshTokenEntity refreshTokenEntity;

  @BeforeEach
  void setUp() {
    userEntity =
        UserEntity.builder()
            .id(1L)
            .name("Kevin")
            .email("test@example.com")
            .password("encoded-password")
            .createdAt(LocalDateTime.now())
            .build();

    refreshTokenEntity =
        RefreshTokenEntity.builder()
            .id(1L)
            .userId(1L)
            .tokenHash("hashed-refresh-token")
            .rawToken("raw-refresh-token")
            .revoked(false)
            .expiresAt(LocalDateTime.now().plusDays(7))
            .createdAt(LocalDateTime.now())
            .build();
  }

  @Test
  void registerUserShouldCreateUserWhenEmailDoesNotExist() {
    // Arrange
    when(userEntityRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

    when(passwordEncoder.encode("password123")).thenReturn("encoded-password");

    when(userService.createUser("Kevin", "test@example.com", "encoded-password"))
        .thenReturn(userEntity);

    // Act
    AuthResponse response = authService.registerUser("Kevin", "test@example.com", "password123");

    // Assert
    assertEquals("User registered successfully ", response.message());
    assertNull(response.token());
    assertNull(response.tokenExpiration());
    assertNull(response.refreshToken());
    assertNull(response.refreshTokenExpiration());

    verify(userEntityRepository).findByEmail("test@example.com");
    verify(passwordEncoder).encode("password123");
    verify(userService).createUser("Kevin", "test@example.com", "encoded-password");

    verifyNoInteractions(authenticationManager);
    verifyNoInteractions(jwtService);
    verifyNoInteractions(refreshTokenService);
  }

  @Test
  void registerUserShouldThrowConflictWhenEmailAlreadyExists() {
    // Arrange
    when(userEntityRepository.findByEmail("test@example.com")).thenReturn(Optional.of(userEntity));

    assertThatThrownBy(() -> authService.registerUser("Kevin", "test@example.com", "password123"))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("Email is already in use");

    verify(userEntityRepository).findByEmail("test@example.com");
    verify(userService, never()).createUser(anyString(), anyString(), anyString());
    verify(passwordEncoder, never()).encode(anyString());

    verifyNoInteractions(authenticationManager);
    verifyNoInteractions(jwtService);
    verifyNoInteractions(refreshTokenService);
  }

  @Test
  void loginUserShouldReturnTokensWhenCredentialsAreValid() {
    // Arrange
    String email = "test@example.com";
    String password = "password123";
    String jwtToken = "jwt-token";
    String jwtExpiration = new Date(System.currentTimeMillis() + 900_000).toString();

    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(null);

    when(customUserDetailsService.loadUserByUsername(email)).thenReturn(userDetails);

    when(jwtService.generateToken(userDetails)).thenReturn(jwtToken);

    when(jwtService.extractExpiration(jwtToken)).thenReturn(jwtExpiration);

    when(refreshTokenService.generateRefreshToken(email)).thenReturn(refreshTokenEntity);

    // Act
    AuthResponse response = authService.loginUser(email, password);

    // Assert
    assertEquals("User logged in successfully", response.message());
    assertEquals(jwtToken, response.token());
    assertEquals(jwtExpiration, response.tokenExpiration());
    assertEquals("raw-refresh-token", response.refreshToken());
    assertEquals(
        Date.from(
                refreshTokenEntity
                    .getExpiresAt()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant())
            .toString(),
        response.refreshTokenExpiration());

    verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    verify(customUserDetailsService).loadUserByUsername(email);
    verify(jwtService).generateToken(userDetails);
    verify(jwtService).extractExpiration(jwtToken);
    verify(refreshTokenService).generateRefreshToken(email);
  }

  @Test
  void loginUserShouldReturnInvalidCredentialsWhenAuthenticationFails() {
    // Arrange
    String email = "test@example.com";
    String password = "wrong-password";

    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenThrow(new BadCredentialsException("Invalid credentials"));

    assertThatThrownBy(() -> authService.loginUser(email, password))
        .isInstanceOf(UnauthorizedException.class)
        .hasMessageContaining("Invalid credentials");

    verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

    verifyNoInteractions(customUserDetailsService);
    verifyNoInteractions(jwtService);
    verifyNoInteractions(refreshTokenService);
  }

  @Test
  void logoutShouldRevokeRefreshTokenAndReturnSuccessMessage() {
    // Arrange
    String refreshToken = "raw-refresh-token";

    // Act
    AuthResponse response = authService.logout(refreshToken);

    // Assert
    assertInstanceOf(AuthResponse.class, response);

    AuthResponse authResponse = (AuthResponse) response;

    assertEquals("User logged out successfully", authResponse.message());
    assertNull(authResponse.token());
    assertNull(authResponse.tokenExpiration());
    assertNull(authResponse.refreshToken());
    assertNull(authResponse.refreshTokenExpiration());

    verify(refreshTokenService).revokeRefreshToken(refreshToken);

    verifyNoInteractions(userService);
    verifyNoInteractions(customUserDetailsService);
    verifyNoInteractions(authenticationManager);
    verifyNoInteractions(passwordEncoder);
    verifyNoInteractions(jwtService);
  }

  @Test
  void refreshShouldReturnNewJwtWhenRefreshTokenIsValid() {
    // Arrange
    String refreshToken = "raw-refresh-token";
    String email = "test@example.com";
    String newJwtToken = "new-jwt-token";
    String jwtExpiration = new Date(System.currentTimeMillis() + 900_000).toString();

    when(refreshTokenService.getRefreshTokenNotRevoked(refreshToken))
        .thenReturn(refreshTokenEntity);

    when(jwtService.generateToken(email)).thenReturn(newJwtToken);

    when(jwtService.extractExpiration(newJwtToken)).thenReturn(jwtExpiration);

    // Act
    AuthResponse response = authService.refresh(refreshToken, email);

    // Assert
    assertEquals("Token refreshed successfully", response.message());
    assertEquals(newJwtToken, response.token());
    assertEquals(jwtExpiration, response.tokenExpiration());
    assertEquals(refreshToken, response.refreshToken());
    assertEquals(refreshTokenEntity.getExpiresAt().toString(), response.refreshTokenExpiration());

    verify(refreshTokenService).getRefreshTokenNotRevoked(refreshToken);
    verify(jwtService).generateToken(email);
    verify(jwtService).extractExpiration(newJwtToken);

    verifyNoInteractions(userService);
    verifyNoInteractions(customUserDetailsService);
    verifyNoInteractions(authenticationManager);
    verifyNoInteractions(passwordEncoder);
  }

  @Test
  void refreshShouldReturnInvalidRefreshTokenWhenRefreshTokenDoesNotExist() {
    // Arrange
    String refreshToken = "bad-refresh-token";
    String email = "test@example.com";

    when(refreshTokenService.getRefreshTokenNotRevoked(refreshToken)).thenReturn(null);

    assertThatThrownBy(() -> authService.refresh(refreshToken, email))
        .isInstanceOf(UnauthorizedException.class)
        .hasMessageContaining("Invalid refresh token");

    verify(refreshTokenService).getRefreshTokenNotRevoked(refreshToken);

    verifyNoInteractions(jwtService);
  }
}
