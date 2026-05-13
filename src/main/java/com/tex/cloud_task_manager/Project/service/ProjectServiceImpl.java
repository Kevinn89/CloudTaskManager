package com.tex.cloud_task_manager.Project.service;

import com.tex.cloud_task_manager.Project.ProjectEntity;
import com.tex.cloud_task_manager.Project.ProjectPriority;
import com.tex.cloud_task_manager.Project.ProjectRepository;
import com.tex.cloud_task_manager.Project.ProjectStatus;
import com.tex.cloud_task_manager.Project.response_request.ProjectResponse;
import com.tex.cloud_task_manager.Security.CurrentUserService;
import com.tex.cloud_task_manager.Task.TaskStatus;
import com.tex.cloud_task_manager.Task.response_request.TaskResponse;
import com.tex.cloud_task_manager.common.exception.BadRequestException;
import com.tex.cloud_task_manager.common.exception.ResourceNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

  private final CurrentUserService currentUserService;
  private final ProjectRepository projectRepository;

  @Override
  public ProjectResponse createProject(String name, String description) {

    long userId = currentUserService.getCurrentUserId();

    ProjectEntity project =
        saveProject(
            ProjectEntity.builder()
                .userId(userId)
                .name(name)
                .description(description)
                .createdAt(LocalDateTime.now())
                .status(ProjectStatus.ACTIVE)
                .priority(ProjectPriority.LOW)
                .build());

    return ProjectResponse.from(
        project,
        project.getTasks().size(),
        project.getTasks().stream().map(TaskResponse::from).toList());
  }

  private ProjectEntity saveProject(ProjectEntity project) {
    return projectRepository.save(project);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProjectResponse> getUserProjects() {

    long userId = currentUserService.getCurrentUserId();

    return projectRepository
        .findByUserId(userId)
        .orElseThrow(() -> new ResourceNotFoundException("No Projects found for user " + userId))
        .stream()
        .map(
            project -> {
              List<TaskResponse> tr =
                  project.getTasks() == null
                      ? List.of()
                      : project.getTasks().stream().map(TaskResponse::from).toList();

              return ProjectResponse.from(project, tr.size(), tr);
            })
        .toList();
  }

  @Override
  @Transactional
  public ProjectResponse deleteProject(long projectId) {

    long userId = currentUserService.getCurrentUserId();

    return projectRepository
        .findByIdAndUserId(projectId, userId)
        .map(
            project -> {
              projectRepository.delete(project);
              return ProjectResponse.from(
                  project,
                  project.getTasks().size(),
                  project.getTasks().stream().map(TaskResponse::from).toList());
            })
        .orElseThrow(
            () -> new ResourceNotFoundException("Project not found with id: " + projectId));
  }

  @Override
  @Transactional
  public ProjectResponse updateProject(long projectId, String name, String description) {

    long userId = currentUserService.getCurrentUserId();

    return projectRepository
        .findByIdAndUserId(projectId, userId)
        .map(
            project -> {
              if (name != null && !name.isBlank()) project.setName(name);
              if (description != null && !description.isBlank())
                project.setDescription(description);

              project.setUpdatedAt(LocalDateTime.now());
              saveProject(project);

              return ProjectResponse.from(
                  project,
                  project.getTasks().size(),
                  project.getTasks().stream().map(TaskResponse::from).toList());
            })
        .orElseThrow(
            () -> new ResourceNotFoundException("Project not found with id: " + projectId));
  }

  @Override
  @Transactional
  public ProjectResponse getProject(long projectId) {

    long userId = currentUserService.getCurrentUserId();

    return projectRepository
        .findByIdAndUserId(projectId, userId)
        .map(
            project -> {
              int count = project.getTasks() == null ? 0 : project.getTasks().size();

              return ProjectResponse.from(
                  project, count, project.getTasks().stream().map(TaskResponse::from).toList());
            })
        .orElseThrow(
            () -> new ResourceNotFoundException("Project not found with id: " + projectId));
  }

  @Override
  @Transactional
  public ProjectResponse completeProject(long projectId) {

    long userId = currentUserService.getCurrentUserId();

    return projectRepository
        .findByIdAndUserId(projectId, userId)
        .map(
            project -> {
              project.getTasks().stream()
                  .forEach(
                      task -> {
                        if (task.getCompletionDate() == null
                            || !task.getTaskStatus().equals(TaskStatus.DONE)) {
                          throw new BadRequestException(
                              "Incomplete Task title "
                                  + task.getTitle()
                                  + " currently "
                                  + task.getTaskStatus());
                        }
                      });

              project.setStatus(ProjectStatus.COMPLETED);
              project.setPriority(ProjectPriority.LOW);
              project.setUpdatedAt(LocalDateTime.now());

              return ProjectResponse.from(project, 0, null);
            })
        .orElseThrow(
            () -> new ResourceNotFoundException("Project not found with id: " + projectId));
  }
}
