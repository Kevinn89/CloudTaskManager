package com.tex.cloud_task_manager.Auth.response_request.Project;

import java.time.LocalDateTime;

import com.tex.cloud_task_manager.Project.ProjectStatus;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        int taskCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        ProjectStatus status
) {
}
