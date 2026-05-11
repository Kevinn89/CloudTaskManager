package com.tex.cloud_task_manager.Project.service;


import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tex.cloud_task_manager.Project.ProjectEntity;
import com.tex.cloud_task_manager.Project.ProjectRepository;
import com.tex.cloud_task_manager.Project.ProjectStatus;
import com.tex.cloud_task_manager.Project.response_request.ProjectResponse;
import com.tex.cloud_task_manager.Security.CurrentUserService;
import com.tex.cloud_task_manager.Task.Priority;
import com.tex.cloud_task_manager.Task.response_request.TaskResponse;
import com.tex.cloud_task_manager.common.exception.ResourceNotFoundException;

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
        .priority(Priority.LOW)
        .build());

        return ProjectResponse.from(project, project.getTasks().size(), project.getTasks().stream().map(TaskResponse::from).toList());

    }

    private ProjectEntity saveProject(ProjectEntity project) {
        return projectRepository.save(project);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> getUserProjects() {

        long userId = currentUserService.getCurrentUserId();

                
        return projectRepository.findByUserId(userId)
        .orElseThrow(() -> new ResourceNotFoundException("No Projects found for user " + userId)
                    ).stream().map(project -> {
                    List<TaskResponse> tr = project.getTasks() == null ? List.of() :
                        project.getTasks().stream().map(TaskResponse::from).toList();

                        return ProjectResponse.from(project, tr.size(), tr);
        }).toList();
    }

    @Override
    @Transactional
    public ProjectResponse deleteProject(long projectId) {

        long userId = currentUserService.getCurrentUserId();

       return projectRepository.findByIdAndUserId(projectId, userId)
                .map(project -> {
            projectRepository.delete(project);
                    return ProjectResponse.from(project, project.getTasks().size(), project.getTasks().stream().map(TaskResponse::from).toList());
        }).orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
    }

    @Override
    @Transactional
    public ProjectResponse updateProject(long projectId, String name, String description) {

        long userId = currentUserService.getCurrentUserId();

        return projectRepository.findByIdAndUserId(projectId, userId).map(project -> {
            project.setName(name);
            project.setDescription(description);
            project.setUpdatedAt(LocalDateTime.now());
            saveProject(project);

            return ProjectResponse.from(project, project.getTasks().size(), project.getTasks().stream().map(TaskResponse::from).toList());

        }).orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
    }

    @Override
    @Transactional
    public ProjectResponse getProject(long projectId) {

      long userId = currentUserService.getCurrentUserId();

      return projectRepository.findByIdAndUserId(projectId, userId).map(project -> {

          int count = project.getTasks() == null ? 0 : project.getTasks().size();

            return ProjectResponse.from(project,count, project.getTasks().stream().map(TaskResponse::from).toList());
           
         }).orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
    }

}
