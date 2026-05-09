package com.tex.cloud_task_manager.Task.response_request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTaskRequest(

        long id,
        
        @Size(max = 100)
        String title,

        @Size(max = 1000)
        String description,

        @NotBlank
        String taskStatus,

        String priority,

        long projectId,

        String dueDate,

        String completionDate

) {

}