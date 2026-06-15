package com.tex.cloud_task_manager.Task.service;

import com.tex.cloud_task_manager.Project.response_request.ProjectResponse;
import com.tex.cloud_task_manager.Task.response_request.TaskResponse;
import java.util.List;

public interface TaskService {

  TaskResponse create(String title, String description, long projectId);

  List<TaskResponse> getTasks(long projectId);

  void deleteTask(long projectId, long taskId);

  TaskResponse updateTask(
      long taskId,
      long projectId,
      String title,
      String description,
      String taskStatus,
      String dueDate,
      String completionDate,
      String priority);

  TaskResponse assignUserTask(long taskId, long userId, long projectId);

}
