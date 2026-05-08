package com.tex.cloud_task_manager.Auth.response_request;

public record AuthResponse(
    String message,
    String token,
    String tokenExpiration,
    String refreshToken,
    String refreshTokenExpiration
) {


}
