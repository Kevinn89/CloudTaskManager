package com.tex.cloud_task_manager.Auth.response_request;

import java.util.List;

public record AuthResponse(
        String message,
        String token,
        String refreshToken,
        List<String> privileges) {
}
