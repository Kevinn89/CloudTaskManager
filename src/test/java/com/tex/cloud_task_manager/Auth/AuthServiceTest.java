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
import com.tex.cloud_task_manager.Notification.UserRegisteredMessage;
import com.tex.cloud_task_manager.Notification.UserRegistrationNotificationPublisher;
import com.tex.cloud_task_manager.RefreshToken.RefreshTokenEntity;
import com.tex.cloud_task_manager.RefreshToken.service.RefreshTokenService;
import com.tex.cloud_task_manager.Security.CustomUserDetailsService;
import com.tex.cloud_task_manager.Security.JwtService;
import com.tex.cloud_task_manager.User.UserEntity;
import com.tex.cloud_task_manager.User.UserEntityRepository;
import com.tex.cloud_task_manager.User.service.UserService;
import com.tex.cloud_task_manager.common.exception.ConflictException;
import com.tex.cloud_task_manager.common.exception.ResourceNotFoundException;
import com.tex.cloud_task_manager.common.exception.UnauthorizedException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

  @Mock private UserRegistrationNotificationPublisher userRegistrationNotificationPublisher;

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
            .accountType("USER")
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

    when(userService.createUser("Kevin", "test@example.com", "encoded-password", "USER"))
        .thenReturn(userEntity);

    // Act
    AuthResponse response =
        authService.registerUser("Kevin", "test@example.com", "password123", "USER");

    // Assert
    assertEquals("User registered successfully ", response.message());
    assertNull(response.token());
    assertNull(response.refreshToken());
    assertNull(response.privileges());

    verify(userEntityRepository).findByEmail("test@example.com");
    verify(passwordEncoder).encode("password123");
    verify(userService).createUser("Kevin", "test@example.com", "encoded-password", "USER");

    ArgumentCaptor<UserRegisteredMessage> messageCaptor =
        ArgumentCaptor.forClass(UserRegisteredMessage.class);
    verify(userRegistrationNotificationPublisher).publish(messageCaptor.capture());
    UserRegisteredMessage message = messageCaptor.getValue();
    assertEquals(1L, message.userId());
    assertEquals("Kevin", message.name());
    assertEquals("test@example.com", message.email());
    assertEquals("USER", message.accountType());
    assertInstanceOf(java.util.UUID.class, message.messageId());
    assertNull(message.traceId());

    verifyNoInteractions(authenticationManager);
    verifyNoInteractions(jwtService);
    verifyNoInteractions(refreshTokenService);
  }

  @Test
  void registerUserShouldThrowConflictWhenEmailAlreadyExists() {
    // Arrange
    when(userEntityRepository.findByEmail("test@example.com")).thenReturn(Optional.of(userEntity));

    assertThatThrownBy(
            () -> authService.registerUser("Kevin", "test@example.com", "password123", "USER"))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("Email is already in use");

    verify(userEntityRepository).findByEmail("test@example.com");
    verify(userService, never()).createUser(anyString(), anyString(), anyString(), anyString());
    verify(passwordEncoder, never()).encode(anyString());
    verifyNoInteractions(userRegistrationNotificationPublisher);

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

    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(null);

    UserDetails standardUserDetails =
        org.springframework.security.core.userdetails.User.withUsername(email)
            .password("encoded-password")
            .roles("USER")
            .build();

    when(customUserDetailsService.loadUserByUsername(email)).thenReturn(standardUserDetails);

    when(jwtService.generateToken(standardUserDetails)).thenReturn(jwtToken);

    when(refreshTokenService.generateRefreshToken(email)).thenReturn(refreshTokenEntity);

    // Act
    AuthResponse response = authService.loginUser(email, password);

    // Assert
    assertEquals("User logged in successfully", response.message());
    assertEquals(jwtToken, response.token());
    assertEquals("raw-refresh-token", response.refreshToken());
    assertEquals(List.of(Privilege.UPDATE, Privilege.READ), response.privileges());

    verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    verify(customUserDetailsService).loadUserByUsername(email);
    verify(jwtService).generateToken(standardUserDetails);
    verify(refreshTokenService).generateRefreshToken(email);
  }

  @Test
  void loginUserShouldReturnAdminPrivilegesWhenUserHasAdminRole() {
    // Arrange
    String email = "admin@example.com";
    String password = "password123";
    String jwtToken = "admin-jwt-token";

    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(null);
    UserDetails adminUserDetails =
        org.springframework.security.core.userdetails.User.withUsername(email)
            .password("encoded-password")
            .roles("ADMIN")
            .build();

    when(customUserDetailsService.loadUserByUsername(email)).thenReturn(adminUserDetails);
    when(jwtService.generateToken(adminUserDetails)).thenReturn(jwtToken);
    when(refreshTokenService.generateRefreshToken(email)).thenReturn(refreshTokenEntity);

    // Act
    AuthResponse response = authService.loginUser(email, password);

    // Assert
    assertEquals(
        List.of(Privilege.CREATE, Privilege.DELETE, Privilege.UPDATE, Privilege.READ),
        response.privileges());
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
    assertNull(authResponse.refreshToken());
    assertNull(authResponse.privileges());

    verify(refreshTokenService).revokeRefreshToken(refreshToken);

    verifyNoInteractions(userService);
    verifyNoInteractions(customUserDetailsService);
    verifyNoInteractions(authenticationManager);
    verifyNoInteractions(passwordEncoder);
    verifyNoInteractions(jwtService);
  }

  @Test
  void userVerifedShouldSaveVerificationInstant() {
    String email = "test@example.com";
    String instantRepresentation = "2026-07-13T15:30:00Z";
    when(userEntityRepository.findByEmail(email)).thenReturn(Optional.of(userEntity));

    authService.userVerifed(email, instantRepresentation);

    assertEquals(Instant.parse(instantRepresentation), userEntity.getVerifiedAt());
    verify(userEntityRepository).findByEmail(email);
    verify(userEntityRepository).save(userEntity);
  }

  @Test
  void userVerifedShouldThrowWhenUserDoesNotExist() {
    String email = "missing@example.com";
    when(userEntityRepository.findByEmail(email)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.userVerifed(email, "2026-07-13T15:30:00Z"))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining(email);

    verify(userEntityRepository).findByEmail(email);
    verify(userEntityRepository, never()).save(any(UserEntity.class));
  }

  @Test
  void refreshShouldReturnNewJwtWhenRefreshTokenIsValid() {
    // Arrange
    String refreshToken = "raw-refresh-token";
    String email = "test@example.com";
    String newJwtToken = "new-jwt-token";

    when(refreshTokenService.getRefreshTokenNotRevoked(refreshToken))
        .thenReturn(refreshTokenEntity);

    when(userEntityRepository.findById(1L)).thenReturn(Optional.of(userEntity));
    when(jwtService.generateToken(email)).thenReturn(newJwtToken);

    // Act
    AuthResponse response = authService.refresh(refreshToken);

    // Assert
    assertEquals("Token refreshed successfully", response.message());
    assertEquals(newJwtToken, response.token());
    assertEquals(refreshToken, response.refreshToken());
    assertNull(response.privileges());

    verify(refreshTokenService).getRefreshTokenNotRevoked(refreshToken);
    verify(userEntityRepository).findById(1L);
    verify(jwtService).generateToken(email);

    verifyNoInteractions(userService);
    verifyNoInteractions(customUserDetailsService);
    verifyNoInteractions(authenticationManager);
    verifyNoInteractions(passwordEncoder);
  }

  @Test
  void refreshShouldReturnInvalidRefreshTokenWhenRefreshTokenDoesNotExist() {
    // Arrange
    String refreshToken = "bad-refresh-token";

    when(refreshTokenService.getRefreshTokenNotRevoked(refreshToken)).thenReturn(null);

    assertThatThrownBy(() -> authService.refresh(refreshToken))
        .isInstanceOf(UnauthorizedException.class)
        .hasMessageContaining("Invalid refresh token");

    verify(refreshTokenService).getRefreshTokenNotRevoked(refreshToken);

    verifyNoInteractions(jwtService);
  }

  @Test
  void refreshShouldThrowUnauthorizedWhenRefreshTokenUserDoesNotExist() {
    String refreshToken = "raw-refresh-token";

    when(refreshTokenService.getRefreshTokenNotRevoked(refreshToken))
        .thenReturn(refreshTokenEntity);
    when(userEntityRepository.findById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.refresh(refreshToken))
        .isInstanceOf(UnauthorizedException.class)
        .hasMessageContaining("Invalid refresh token");

    verify(refreshTokenService).getRefreshTokenNotRevoked(refreshToken);
    verify(userEntityRepository).findById(1L);
    verifyNoInteractions(jwtService);
  }
}
