package com.tex.cloud_task_manager.Auth.response_request;

import com.tex.cloud_task_manager.Auth.Privilege;
import java.util.List;

public record AuthResponse(
    String message, String token, String refreshToken, List<Privilege> privileges) {}
