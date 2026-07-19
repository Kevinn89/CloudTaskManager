package com.tex.cloud_task_manager.Auth;

import com.tex.cloud_task_manager.Auth.response_request.AuthResponse;
import com.tex.cloud_task_manager.Auth.response_request.LoginRequest;
import com.tex.cloud_task_manager.Auth.service.AuthService;
import com.tex.cloud_task_manager.Config.JwtProperties;
import com.tex.cloud_task_manager.User.response_request.RegisterRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private static final String ACCESS_TOKEN_NAME = "access_token";
  private static final String REFRESH_TOKEN_NAME = "refresh_token";

  private static final String ACCESS_TOKEN_PATH = "/api";
  private static final String REFRESH_TOKEN_PATH = "/api/auth";

  private final AuthService authService;
  private final JwtProperties jwtProps;

  @PostMapping("/refresh")
  public ResponseEntity<Void> refresh(HttpServletRequest request) {

    String refreshToken = getCookieValue(request, REFRESH_TOKEN_NAME);

    if (refreshToken == null || refreshToken.isBlank()) {

      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    AuthResponse newAccessToken = authService.refresh(refreshToken);

    ResponseCookie accessTokenCookie =
        ResponseCookie.from(ACCESS_TOKEN_NAME, newAccessToken.token())
            .httpOnly(true)
            .secure(false)
            .path(ACCESS_TOKEN_PATH)
            .maxAge(Duration.ofMinutes(jwtProps.accessTokenExpirationMinutes()))
            .sameSite("Lax")
            .build();

    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
        .build();
  }

  private String getCookieValue(HttpServletRequest request, String cookieName) {

    if (request.getCookies() == null) {

      return null;
    }

    return Arrays.stream(request.getCookies())
        .filter(cookie -> cookie.getName().equals(cookieName))
        .map(Cookie::getValue)
        .findFirst()
        .orElse(null);
  }

  @PostMapping("/register")
  public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    return ResponseEntity.ok(
        authService.registerUser(
            request.name(), request.email(), request.password(), request.accountType()));
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    AuthResponse response = authService.loginUser(request.email(), request.password());

    ResponseCookie accessTokenCookie =
        ResponseCookie.from(ACCESS_TOKEN_NAME, response.token())
            .httpOnly(true)
            .secure(false) // true in production HTTPS
            .path(ACCESS_TOKEN_PATH)
            .maxAge(Duration.ofMinutes(jwtProps.accessTokenExpirationMinutes()))
            .sameSite("Lax")
            .build();

    ResponseCookie refreshTokenCookie =
        ResponseCookie.from(REFRESH_TOKEN_NAME, response.refreshToken())
            .httpOnly(true)
            .secure(false) // true in production HTTPS
            .path(REFRESH_TOKEN_PATH)
            .maxAge(Duration.ofDays(jwtProps.refreshTokenExpirationDays()))
            .sameSite("Lax")
            .build();

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
        .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
        .body(new AuthResponse("Successfully Login", null, null, response.privileges()));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletRequest request) {

    String refreshToken = getCookieValue(request, REFRESH_TOKEN_NAME);

    if (refreshToken == null || refreshToken.isBlank()) {

      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    authService.logout(refreshToken);

    ResponseCookie clearAccessTokenCookie =
        ResponseCookie.from(ACCESS_TOKEN_NAME, "")
            .httpOnly(true)
            .secure(false)
            .path(ACCESS_TOKEN_PATH)
            .maxAge(0)
            .sameSite("Lax")
            .build();

    ResponseCookie clearRefreshTokenCookie =
        ResponseCookie.from(REFRESH_TOKEN_NAME, "")
            .httpOnly(true)
            .secure(false)
            .path(REFRESH_TOKEN_PATH)
            .maxAge(0)
            .sameSite("Lax")
            .build();

    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, clearAccessTokenCookie.toString())
        .header(HttpHeaders.SET_COOKIE, clearRefreshTokenCookie.toString())
        .build();
  }
}
