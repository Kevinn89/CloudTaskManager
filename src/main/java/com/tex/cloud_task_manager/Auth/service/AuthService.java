package com.tex.cloud_task_manager.Auth.service;

import com.tex.cloud_task_manager.Auth.response_request.AuthResponse;

public interface AuthService {

  AuthResponse registerUser(String name, String email, String password);

  AuthResponse loginUser(String email, String password);

  AuthResponse logout(String token);

  AuthResponse refresh(String refreshToken, String email);
}
