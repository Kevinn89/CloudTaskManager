package com.tex.cloud_task_manager.Auth.response_request.Task;

public record UpdateTaskStatusRequest(
      long taskId,
      String status
) {

}
