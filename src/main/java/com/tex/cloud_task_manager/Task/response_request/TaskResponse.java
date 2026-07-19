package com.tex.cloud_task_manager.Task.response_request;

import com.tex.cloud_task_manager.Task.TaskEntity;
import com.tex.cloud_task_manager.Task.TaskPriority;
import com.tex.cloud_task_manager.Task.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskResponse(
    Long id,
    Long projectId,
    Long userId,
    Long assignedUserid,
    String title,
    String description,
    TaskStatus taskStatus,
    TaskPriority priority,
    LocalDate dueDate,
    LocalDate completionDate,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {
  public static TaskResponse from(TaskEntity task) {
    return new TaskResponse(
        task.getId(),
        task.getProject().getId(),
        task.getUserId(),
        task.getAssignedUserid(),
        task.getTitle(),
        task.getDescription(),
        task.getTaskStatus(),
        task.getPriority(),
        task.getDueDate(),
        task.getCompletionDate(),
        task.getCreatedAt(),
        task.getUpdatedAt());
  }
}
