package com.tex.cloud_task_manager.Auth.service;

import java.time.ZoneId;
import java.util.Date;
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
import com.tex.cloud_task_manager.User.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {


    private final UserService userService;
    private final CustomUserDetailsService customUserDetailsService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Override
    public AuthResponse registerUser(String name, String email, String password) {
       
        if(userService.findByEmail(email).isPresent()) {

            return new AuthResponse("User already exists for this email", null, null, null, null);
        }

          String encodedPassword = passwordEncoder.encode(password);
        userService.createUser(name, email, encodedPassword);
        return new AuthResponse("User registered successfully ", null, null, null, null);
    }

    @Override
    public AuthResponse loginUser(String email, String password) {
  
        try {

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            var userDetails = customUserDetailsService.loadUserByUsername(email);

            String token = jwtService.generateToken(userDetails);

            var refreshToken = refreshTokenService.generateRefreshToken(email);

            return new AuthResponse("User logged in successfully", token, jwtService.extractExpiration(token), refreshToken.getRawToken(), Date.from(refreshToken.getExpiresAt().atZone(ZoneId.systemDefault()).toInstant()).toString());

        } 
        catch(BadCredentialsException e) {
            return new AuthResponse("Invalid credentials", null, null, null, null);
        }

    }

    @Override
    public AuthResponse logout(String token) {

         refreshTokenService.revokeRefreshToken(token);
         return new AuthResponse("User logged out successfully", null, null, null, null);

    }

    @Override
    public AuthResponse refresh(String refreshToken, String email) {
        var refresh = Optional.ofNullable(
            refreshTokenService.getRefreshTokenNotRevoked(refreshToken));
        if (!refresh.isPresent()) {
            return new AuthResponse("Invalid refresh token", null, null, null, null);
        }
        var newToken = jwtService.generateToken(email);
        return new AuthResponse("Token refreshed successfully", newToken, jwtService.extractExpiration(newToken), refreshToken, refresh.get().getExpiresAt().toString());
    }

    

}
