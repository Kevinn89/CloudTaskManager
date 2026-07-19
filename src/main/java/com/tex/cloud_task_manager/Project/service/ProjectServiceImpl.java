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
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectServiceImpl implements ProjectService {

  private final CurrentUserService currentUserService;
  private final ProjectRepository projectRepository;

  @Override
  @CacheEvict(cacheNames = "userProjects", key = "@currentUserService.getCurrentUserId()")
  public ProjectResponse createProject(String name, String description) {

    long userId = currentUserService.getCurrentUserId();
    log.debug("Creating project for userId={}", userId);

    ProjectEntity project =
        saveProject(
            ProjectEntity.builder()
                .userId(userId)
                .name(name)
                .description(description)
                .createdAt(LocalDateTime.now())
                .status(ProjectStatus.NOT_ACTIVE)
                .priority(ProjectPriority.LOW)
                .build());

    List<TaskResponse> tasks =
        project.getTasks() != null
            ? project.getTasks().stream().map(TaskResponse::from).toList()
            : null;
    int taskCount = project.getTasks() != null ? project.getTasks().size() : 0;

    log.info(
        "Project created successfully with projectId={} for userId={}", project.getId(), userId);
    return ProjectResponse.from(project, taskCount, tasks);
  }

  private ProjectEntity saveProject(ProjectEntity project) {
    return projectRepository.save(project);
  }

  @Override
  @Transactional(readOnly = true)
  @Cacheable(cacheNames = "userProjects", key = "@currentUserService.getCurrentUserId()")
  public List<ProjectResponse> getUserProjects() {

    long userId = currentUserService.getCurrentUserId();

    List<ProjectResponse> projects =
        projectRepository
            .findByUserId(userId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "No Projects found for user %d".formatted(userId)))
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
    log.debug("Retrieved {} projects for userId={}", projects.size(), userId);
    return projects;
  }

  @Override
  @Transactional
  @Caching(
      evict = {
        @CacheEvict(
            cacheNames = "projectById",
            key = "@currentUserService.getCurrentUserId() + ':' + #projectId"),
        @CacheEvict(
            cacheNames = "tasksByProject",
            key = "@currentUserService.getCurrentUserId() + ':' + #projectId"),
        @CacheEvict(cacheNames = "userProjects", key = "@currentUserService.getCurrentUserId()")
      })
  public void deleteProject(long projectId) {

    long userId = currentUserService.getCurrentUserId();
    log.debug("Deleting projectId={} for userId={}", projectId, userId);

    ProjectEntity projectEntity =
        projectRepository
            .findByIdAndUserId(projectId, userId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Project not found with id: %d".formatted(projectId)));

    projectRepository.delete(projectEntity);
    log.info("Project deleted successfully with projectId={} for userId={}", projectId, userId);
  }

  @Override
  @Transactional
  @Caching(
      evict = {
        @CacheEvict(
            cacheNames = "projectById",
            key = "@currentUserService.getCurrentUserId() + ':' + #projectId"),
        @CacheEvict(cacheNames = "userProjects", key = "@currentUserService.getCurrentUserId()")
      })
  public ProjectResponse updateProject(
      long projectId, String name, String description, String priority, String status) {

    long userId = currentUserService.getCurrentUserId();
    log.debug("Updating projectId={} for userId={}", projectId, userId);

    return projectRepository
        .findByIdAndUserId(projectId, userId)
        .map(
            project -> {
              if (name != null && !name.isBlank()) project.setName(name);
              if (description != null && !description.isBlank())
                project.setDescription(description);
              if (priority != null
                  && !priority.isBlank()
                  && !project.getPriority().equals(ProjectPriority.valueOf(priority)))
                project.setPriority(ProjectPriority.valueOf(priority));
              if (status != null
                  && !status.isBlank()
                  && !project.getStatus().equals(ProjectStatus.valueOf(status)))
                project = updateStatus(project, ProjectStatus.valueOf(status));
              project.setUpdatedAt(LocalDateTime.now());
              saveProject(project);

              log.info(
                  "Project updated successfully with projectId={} for userId={}",
                  projectId,
                  userId);

              return ProjectResponse.from(
                  project,
                  project.getTasks().size(),
                  project.getTasks().stream().map(TaskResponse::from).toList());
            })
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "Project not found with id: %d".formatted(projectId)));
  }

  private ProjectEntity updateStatus(ProjectEntity curreStatus, ProjectStatus toStatus) {

    ProjectStatus status = curreStatus.getStatus();
    log.debug("Changing projectId={} status from {} to {}", curreStatus.getId(), status, toStatus);

    switch (status) {
      case ProjectStatus.NOT_ACTIVE:
        if (ProjectStatus.ACTIVE.equals(toStatus)) status = toStatus;
        else {
          log.warn(
              "Rejected projectId={} status transition from {} to {}",
              curreStatus.getId(),
              status,
              toStatus);
          throw new BadRequestException(
              "Unable to move to %s from %s, to be ACTIVE first"
                  .formatted(ProjectStatus.NOT_ACTIVE, toStatus));
        }
        break;
      case ProjectStatus.ACTIVE:
        if (ProjectStatus.COMPLETED.equals(toStatus))
          curreStatus.setCompletedAt(LocalDateTime.now());
        status = toStatus;
        break;
      case ProjectStatus.COMPLETED:
        if (ProjectStatus.ACTIVE.equals(toStatus)) curreStatus.setCompletedAt(null);
        status = toStatus;
      default:
        break;
    }
    curreStatus.setStatus(status);
    return curreStatus;
  }

  @Override
  @Transactional
  @Cacheable(
      cacheNames = "projectById",
      key = "@currentUserService.getCurrentUserId() + ':' + #projectId")
  public ProjectResponse getProject(long projectId) {

    long userId = currentUserService.getCurrentUserId();
    log.debug("Retrieving projectId={} for userId={}", projectId, userId);

    return projectRepository
        .findByIdAndUserId(projectId, userId)
        .map(
            project -> {
              int count = project.getTasks() == null ? 0 : project.getTasks().size();

              log.debug(
                  "Retrieved projectId={} with {} tasks for userId={}", projectId, count, userId);

              return ProjectResponse.from(
                  project, count, project.getTasks().stream().map(TaskResponse::from).toList());
            })
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "Project not found with id: %d".formatted(projectId)));
  }

  @Override
  @Transactional
  @Caching(
      evict = {
        @CacheEvict(
            cacheNames = "projectById",
            key = "@currentUserService.getCurrentUserId() + ':' + #projectId"),
        @CacheEvict(cacheNames = "userProjects", key = "@currentUserService.getCurrentUserId()")
      })
  public ProjectResponse completeProject(long projectId) {

    long userId = currentUserService.getCurrentUserId();
    log.debug("Completing projectId={} for userId={}", projectId, userId);

    return projectRepository
        .findByIdAndUserId(projectId, userId)
        .map(
            project -> {
              project.getTasks().stream()
                  .forEach(
                      task -> {
                        if (task.getCompletionDate() == null
                            || !task.getTaskStatus().equals(TaskStatus.DONE)) {
                          log.warn(
                              "Cannot complete projectId={} because taskId={} is incomplete",
                              projectId,
                              task.getId());
                          throw new BadRequestException(
                              "Incomplete Task title %s currently %s"
                                  .formatted(task.getTitle(), task.getTaskStatus()));
                        }
                      });

              project.setStatus(ProjectStatus.COMPLETED);
              project.setPriority(ProjectPriority.LOW);
              project.setUpdatedAt(LocalDateTime.now());

              log.info(
                  "Project completed successfully with projectId={} for userId={}",
                  projectId,
                  userId);

              return ProjectResponse.from(project, 0, null);
            })
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "Project not found with id: %d".formatted(projectId)));
  }
}
