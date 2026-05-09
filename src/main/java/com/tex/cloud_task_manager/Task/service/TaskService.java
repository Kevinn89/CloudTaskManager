package com.tex.cloud_task_manager.Task.service;

import java.util.List;

import com.tex.cloud_task_manager.Task.response_request.TaskResponse;

public interface TaskService {

    TaskResponse create(String title, String description, long projectId);

    List<TaskResponse> getTasks(long projectId);

    void deleteTask(long projectId, long taskId);

    TaskResponse updateTask(long taskId, long projectId, String title, String description, String taskStatus,
            String dueDate, String completionDate, String priority);

}
