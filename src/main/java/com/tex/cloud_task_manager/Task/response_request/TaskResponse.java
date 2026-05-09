package com.tex.cloud_task_manager.Task.response_request;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.tex.cloud_task_manager.Task.TaskEntity;


public record TaskResponse(
        Long id,
        Long projectId,
        Long userId,
        String title,
        String description,
        String status,
        String priority,
        LocalDate dueDate,
        LocalDate completionDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TaskResponse from(TaskEntity task) {
        return new TaskResponse(
                task.getId(),
                task.getProjectId(),
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus().name(),
                task.getPriority().name(),
                task.getDueDate(),
                task.getCompletionDate(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}