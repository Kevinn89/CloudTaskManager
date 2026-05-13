package com.tex.cloud_task_manager.Auth;

import com.tex.cloud_task_manager.Auth.response_request.AuthResponse;
import com.tex.cloud_task_manager.Auth.response_request.LogOutRequest;
import com.tex.cloud_task_manager.Auth.response_request.LoginRequest;
import com.tex.cloud_task_manager.Auth.service.AuthService;
import com.tex.cloud_task_manager.RefreshToken.response_request.RefreshTokenRequest;
import com.tex.cloud_task_manager.User.response_request.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  @PostMapping("/register")
  public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    return ResponseEntity.ok(
        authService.registerUser(request.name(), request.email(), request.password()));
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(authService.loginUser(request.email(), request.password()));
  }

  @PostMapping("/logout")
  public ResponseEntity<AuthResponse> logout(@Valid @RequestBody LogOutRequest request) {
    return ResponseEntity.ok(authService.logout(request.refreshToken()));
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
    return ResponseEntity.ok(authService.refresh(request.refreshToken(), request.email()));
  }
}
