package com.tex.cloud_task_manager.Project.response_request;

import com.tex.cloud_task_manager.Project.ProjectEntity;
import com.tex.cloud_task_manager.Project.ProjectPriority;
import com.tex.cloud_task_manager.Project.ProjectStatus;
import com.tex.cloud_task_manager.Task.response_request.TaskResponse;
import java.time.LocalDateTime;
import java.util.List;

public record ProjectResponse(
    Long id,
    String name,
    String description,
    int taskCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    ProjectStatus status,
    ProjectPriority priority,
    List<TaskResponse> tasks) {

  public static ProjectResponse from(ProjectEntity task, int count, List<TaskResponse> tasks) {
    return new ProjectResponse(
        task.getId(),
        task.getName(),
        task.getDescription(),
        count,
        task.getCreatedAt(),
        task.getUpdatedAt(),
        task.getStatus(),
        task.getPriority(),
        tasks);
  }
}
