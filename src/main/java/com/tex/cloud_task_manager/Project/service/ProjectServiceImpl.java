package com.tex.cloud_task_manager.Project.service;


import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.tex.cloud_task_manager.Auth.response_request.Project.ProjectResponse;
import com.tex.cloud_task_manager.Project.ProjectEntity;
import com.tex.cloud_task_manager.Project.ProjectRepository;
import com.tex.cloud_task_manager.Project.ProjectStatus;
import com.tex.cloud_task_manager.Security.CurrentUserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final CurrentUserService currentUserService;
    private final ProjectRepository projectRepository;

    @Override
    public ProjectResponse createProject(String name, String description) {

        long userId = currentUserService.getCurrentUserId();

         ProjectEntity project = saveProject(ProjectEntity.builder()
        .userId(userId)
        .name(name)
        .description(description)
        .createdAt(LocalDateTime.now())
        .status(ProjectStatus.ACTIVE)
        .build());

        return new ProjectResponse(project.getId(), project.getName(), project.getDescription(), 0, project.getCreatedAt(), project.getUpdatedAt(), project.getStatus());

    }

    private ProjectEntity saveProject(ProjectEntity project) {
        return projectRepository.save(project);
    }

    @Override
    public List<ProjectResponse> getUserProjects() {

      long userId = currentUserService.getCurrentUserId();

      List<ProjectEntity> projects =  projectRepository.findByUserId(userId);
        
      return projects.stream().map(project -> new ProjectResponse(project.getId(), project.getName(), project.getDescription(), 0, project.getCreatedAt(), project.getUpdatedAt(), project.getStatus())).toList();

    }

    @Override
    public ProjectResponse deleteProject(long projectId) {

        long userId = currentUserService.getCurrentUserId();

       return projectRepository.findByIdAndUserId(projectId, userId).map(project -> {
            projectRepository.delete(project);
            return new ProjectResponse(project.getId(), project.getName(), project.getDescription(), 0, project.getCreatedAt(), project.getUpdatedAt(), project.getStatus());
        }).orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));
    }

    @Override
    public ProjectResponse updateProject(long projectId, String name, String description) {

        long userId = currentUserService.getCurrentUserId();

        return projectRepository.findByIdAndUserId(projectId, userId).map(project -> {
            project.setName(name);
            project.setDescription(description);
            saveProject(project);
            return new ProjectResponse(project.getId(), project.getName(), project.getDescription(), 0, project.getCreatedAt(), project.getUpdatedAt(), project.getStatus());
        }).orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));
    }

    @Override
    public ProjectResponse getProject(long projectId) {

        long userId = currentUserService.getCurrentUserId();

        if(userId == 0) {
            throw new RuntimeException("User not found");
        }

        return projectRepository.findByIdAndUserId(projectId, userId).map(project -> new ProjectResponse(project.getId(), project.getName(), project.getDescription(), 0, project.getCreatedAt(), project.getUpdatedAt(), project.getStatus()))
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));
    }

}
