package com.tex.cloud_task_manager.Project.service;

import com.tex.cloud_task_manager.Project.response_request.ProjectResponse;
import java.util.List;

public interface ProjectService {

  ProjectResponse createProject(String name, String description);

  List<ProjectResponse> getUserProjects();

  ProjectResponse deleteProject(long projectId);

  ProjectResponse updateProject(long projectId, String name, String description);

  ProjectResponse getProject(long projectId);
}
