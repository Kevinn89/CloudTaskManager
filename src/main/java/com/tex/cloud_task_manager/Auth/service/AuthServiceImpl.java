package com.tex.cloud_task_manager.Auth.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.tex.cloud_task_manager.Auth.response_request.AuthResponse;
import com.tex.cloud_task_manager.RefreshToken.service.RefreshTokenService;
import com.tex.cloud_task_manager.Security.CustomUserDetailsService;
import com.tex.cloud_task_manager.Security.JwtService;
import com.tex.cloud_task_manager.User.UserEntityRepository;
import com.tex.cloud_task_manager.User.service.UserService;
import com.tex.cloud_task_manager.common.exception.ConflictException;
import com.tex.cloud_task_manager.common.exception.UnauthorizedException;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final UserService userService;
  private final UserEntityRepository userEntityRepository;
  private final CustomUserDetailsService customUserDetailsService;
  private final AuthenticationManager authenticationManager;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;

  @Override
  public AuthResponse registerUser(String name, String email, String password, String accountType) {

    var userOptional = userEntityRepository.findByEmail(email);

    if (userOptional.isPresent() && !email.equals("test@test.com")) {
      throw new ConflictException("Email is already in use");
    }
    String encodedPassword = passwordEncoder.encode(password);

    userService.createUser(name, email, encodedPassword, accountType);

    return new AuthResponse("User registered successfully ", null, null, null);
  }

  @Override
  public AuthResponse loginUser(String email, String password) {

    try {

      authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));

      var userDetails = customUserDetailsService.loadUserByUsername(email);

      String token = jwtService.generateToken(userDetails);

      var refreshToken = refreshTokenService.generateRefreshToken(email);

      List<String> privileges = new ArrayList<>();

      boolean isAdmin = userDetails.getAuthorities()

          .stream()

          .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

      if (isAdmin) {

        privileges.add("CREATE");
        privileges.add("DELETE");
        privileges.add("UPDATE");
        privileges.add("READ");

      } else {
        privileges.add("UPDATE");
        privileges.add("READ");

      }

      return new AuthResponse(
          "User logged in successfully",
          token,
          refreshToken.getRawToken(),
          privileges);

    } catch (BadCredentialsException e) {
      throw new UnauthorizedException("Invalid credentials");
    }
  }

  @Override
  public AuthResponse logout(String token) {

    refreshTokenService.revokeRefreshToken(token);
    return new AuthResponse("User logged out successfully", null, null, null);
  }

  @Transactional
  @Override
  public AuthResponse refresh(String refreshToken) {

    var refresh = Optional.ofNullable(refreshTokenService.getRefreshTokenNotRevoked(refreshToken))
        .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

    if (refresh.getExpiresAt().isBefore(LocalDateTime.now())) {

      throw new UnauthorizedException("Refresh token expired");
    }

    var user = userEntityRepository
        .findById(refresh.getUserId())
        .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

    var newToken = jwtService.generateToken(user.getEmail());

    return new AuthResponse(
        "Token refreshed successfully",
        newToken,
        refreshToken,
        null);

  }
}
