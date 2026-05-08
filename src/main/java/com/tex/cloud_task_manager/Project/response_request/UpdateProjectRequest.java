package com.tex.cloud_task_manager.Project.response_request;

import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(

        long projectId,
        @Size(max = 100, message = "Project name cannot exceed 100 characters")
        String name,

        @Size(max = 500, message = "Project description cannot exceed 500 characters")
        String description
) {
}
