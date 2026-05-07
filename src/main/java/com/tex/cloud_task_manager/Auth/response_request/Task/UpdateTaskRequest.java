package com.tex.cloud_task_manager.Auth.response_request.Task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTaskRequest(
        @Size(max = 100)
        String title,

        @Size(max = 1000)
        String description,

        @NotBlank
        String status
) {
}