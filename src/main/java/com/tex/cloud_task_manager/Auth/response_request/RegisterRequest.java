package com.tex.cloud_task_manager.Auth.response_request;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
       @NotBlank String email,
       @NotBlank String password,
       @NotBlank String name,
        String createdAt

) {
}
