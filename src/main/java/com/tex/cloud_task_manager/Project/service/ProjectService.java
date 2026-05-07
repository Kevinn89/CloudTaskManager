package com.tex.cloud_task_manager.Project.service;

import java.util.List;

import com.tex.cloud_task_manager.Auth.response_request.Project.ProjectResponse;

public interface ProjectService {

    ProjectResponse createProject(
            String name,
            String description);

    List<ProjectResponse> getUserProjects();

    ProjectResponse deleteProject(long projectId);

    ProjectResponse updateProject(long projectId,
            String name,
            String description);

    ProjectResponse getProject(long projectId);

}
