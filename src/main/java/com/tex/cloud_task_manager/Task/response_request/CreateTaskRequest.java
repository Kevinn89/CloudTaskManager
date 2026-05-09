package com.tex.cloud_task_manager.Task.response_request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(


        long projectId,
        @NotBlank
        @Size(max = 100)
        String title,

        @NotBlank
        @Size(max = 1000)
        String description
) {
}
