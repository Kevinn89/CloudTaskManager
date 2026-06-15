package com.tex.cloud_task_manager.Project.service;

import java.util.List;

import com.tex.cloud_task_manager.Project.response_request.ProjectResponse;

public interface ProjectService {

  ProjectResponse createProject(String name, String description);

  List<ProjectResponse> getUserProjects();

  void deleteProject(long projectId);

  ProjectResponse updateProject(long projectId, String name, String description, String priority,
      String status);

  ProjectResponse getProject(long projectId);

  ProjectResponse completeProject(long projectId);

}
