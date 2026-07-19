package com.tex.cloud_task_manager.RefreshToken.response_request;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(@NotBlank String email) {}
