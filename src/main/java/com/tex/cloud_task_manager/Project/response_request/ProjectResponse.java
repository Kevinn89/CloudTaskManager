package com.tex.cloud_task_manager.Project.response_request;

import java.time.LocalDateTime;
import java.util.List;

import com.tex.cloud_task_manager.Project.ProjectEntity;
import com.tex.cloud_task_manager.Project.ProjectStatus;
import com.tex.cloud_task_manager.Task.Priority;
import com.tex.cloud_task_manager.Task.response_request.TaskResponse;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        int taskCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        ProjectStatus status,
        Priority priority,
        List<TaskResponse> tasks
) {

         public static ProjectResponse from(ProjectEntity task , int count, List<TaskResponse> tasks ) {
        return new ProjectResponse(
                task.getId(),
                task.getName(),
                task.getDescription(),
                count,
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getStatus(),
                task.getPriority(),
                tasks 
        );
    }
   
}
