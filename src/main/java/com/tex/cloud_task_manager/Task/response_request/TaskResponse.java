package com.tex.cloud_task_manager.Task.response_request;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.tex.cloud_task_manager.Task.Priority;
import com.tex.cloud_task_manager.Task.TaskEntity;
import com.tex.cloud_task_manager.Task.TaskStatus;


public record TaskResponse(
        Long id,
        Long projectId,
        Long userId,
        String title,
        String description,
        TaskStatus taskStatus,
        Priority priority,
        LocalDate dueDate,
        LocalDate completionDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TaskResponse from(TaskEntity task) {
        return new TaskResponse(
                task.getId(),
                task.getProject().getId(),
                task.getUserId(),
                task.getTitle(),
                task.getDescription(),
                task.getTaskStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getCompletionDate(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
