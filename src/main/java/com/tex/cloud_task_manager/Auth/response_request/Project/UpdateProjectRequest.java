package com.tex.cloud_task_manager.Auth.response_request.Project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(

        @NotBlank(message = "Project ID is required")
        long projectId,
        @Size(max = 100, message = "Project name cannot exceed 100 characters")
        String name,

        @Size(max = 500, message = "Project description cannot exceed 500 characters")
        String description
) {
}
