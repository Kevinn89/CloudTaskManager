package com.tex.cloud_task_manager.Task.service;

import com.tex.cloud_task_manager.Project.ProjectRepository;
import com.tex.cloud_task_manager.Security.CurrentUserService;
import com.tex.cloud_task_manager.Task.TaskEntity;
import com.tex.cloud_task_manager.Task.TaskPriority;
import com.tex.cloud_task_manager.Task.TaskRepository;
import com.tex.cloud_task_manager.Task.TaskStatus;
import com.tex.cloud_task_manager.Task.response_request.TaskResponse;
import com.tex.cloud_task_manager.common.exception.BadRequestException;
import com.tex.cloud_task_manager.common.exception.ResourceNotFoundException;
import com.tex.cloud_task_manager.common.exception.UnauthorizedException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskServiceImpl implements TaskService {

  private final TaskRepository taskRepository;
  private final ProjectRepository projectRepository;
  private final CurrentUserService currentUserService;

  private long getCurrentUserId() {

    return currentUserService.getCurrentUserId();
  }

  @Override
  @Caching(
      evict = {
        @CacheEvict(
            cacheNames = "tasksByProject",
            key = "@currentUserService.getCurrentUserId() + ':' + #projectId"),
        @CacheEvict(
            cacheNames = "projectById",
            key = "@currentUserService.getCurrentUserId() + ':' + #projectId"),
        @CacheEvict(cacheNames = "userProjects", key = "@currentUserService.getCurrentUserId()")
      })
  public TaskResponse create(String title, String description, long projectId) {

    long userId = getCurrentUserId();
    log.debug("Creating task for projectId={} and userId={}", projectId, userId);

    var project =
        projectRepository
            .findByIdAndUserId(projectId, userId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Project not found for projectId %d".formatted(projectId)));

    TaskEntity taskEntity =
        TaskEntity.builder()
            .title(title)
            .project(project)
            .userId(userId)
            .description(description)
            .priority(TaskPriority.LOW)
            .taskStatus(TaskStatus.TODO)
            .createdAt(LocalDateTime.now())
            .build();

    TaskEntity savedTask = taskRepository.save(taskEntity);
    log.info(
        "Task created successfully with taskId={} for projectId={}", savedTask.getId(), projectId);
    return TaskResponse.from(savedTask);
  }

  @Override
  @Cacheable(
      cacheNames = "tasksByProject",
      key = "@currentUserService.getCurrentUserId() + ':' + #projectId")
  public List<TaskResponse> getTasks(long projectId) {
    long userId = getCurrentUserId();
    log.debug("Retrieving tasks for projectId={} and userId={}", projectId, userId);

    var project =
        projectRepository
            .findByIdAndUserId(projectId, userId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Project not found for projectId %d".formatted(projectId)));

    List<TaskEntity> taskEntity =
        taskRepository.findByProjectIdAndUserId(projectId, project.getUserId());

    log.debug("Retrieved {} tasks for projectId={}", taskEntity.size(), projectId);
    return taskEntity.stream().map(TaskResponse::from).toList();
  }

  @Override
  @Caching(
      evict = {
        @CacheEvict(
            cacheNames = "tasksByProject",
            key = "@currentUserService.getCurrentUserId() + ':' + #projectId"),
        @CacheEvict(
            cacheNames = "projectById",
            key = "@currentUserService.getCurrentUserId() + ':' + #projectId"),
        @CacheEvict(cacheNames = "userProjects", key = "@currentUserService.getCurrentUserId()")
      })
  public TaskResponse updateTask(
      long taskId,
      long projectId,
      String title,
      String description,
      String taskStatus,
      String dueDate,
      String completionDate,
      String priority) {
    long userId = getCurrentUserId();
    log.debug("Updating taskId={} for projectId={} and userId={}", taskId, projectId, userId);

    var project =
        projectRepository
            .findByIdAndUserId(projectId, userId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Project not found for projectId %d".formatted(projectId)));

    TaskEntity taskEntity =
        taskRepository
            .findByIdAndProjectIdAndUserId(taskId, project.getId(), project.getUserId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Task not found for project %d".formatted(projectId)));

    if (description != null && !description.isBlank()) taskEntity.setDescription(description);
    if (dueDate != null && !dueDate.isBlank()) taskEntity.setDueDate(LocalDate.parse(dueDate));
    if (title != null && !title.isBlank()) taskEntity.setTitle(title);
    if (taskStatus != null
        && !taskStatus.isBlank()
        && !taskEntity.getTaskStatus().equals(TaskStatus.valueOf(taskStatus)))
      taskEntity = updateStatus(taskEntity, TaskStatus.valueOf(taskStatus));
    if (completionDate != null && !completionDate.isBlank())
      taskEntity.setCompletionDate(LocalDate.parse(completionDate));
    if (priority != null
        && !priority.isBlank()
        && !taskEntity.getPriority().equals(TaskPriority.valueOf(priority)))
      taskEntity.setPriority(TaskPriority.valueOf(priority));
    if (project != null) taskEntity.setProject(project);

    TaskEntity savedTask = taskRepository.save(taskEntity);
    log.info("Task updated successfully with taskId={} for projectId={}", taskId, projectId);
    return TaskResponse.from(savedTask);
  }

  private TaskEntity updateStatus(TaskEntity curreStatus, TaskStatus toStatus) {

    TaskStatus status = curreStatus.getTaskStatus();
    log.debug("Changing taskId={} status from {} to {}", curreStatus.getId(), status, toStatus);

    switch (status) {
      case TaskStatus.TODO:
        if (TaskStatus.IN_PROGRESS.equals(toStatus)) status = toStatus;
        else {
          log.warn(
              "Rejected taskId={} status transition from {} to {}",
              curreStatus.getId(),
              status,
              toStatus);
          throw new BadRequestException(
              "Unable to move to %s from %s".formatted(TaskStatus.IN_PROGRESS, status));
        }
        break;
      case TaskStatus.IN_PROGRESS:
        if (TaskStatus.DONE.equals(toStatus)) curreStatus.setCompletionDate(LocalDate.now());
        status = toStatus;
        if (TaskStatus.TODO.equals(toStatus)) status = toStatus;
      case TaskStatus.DONE:
        if (TaskStatus.IN_PROGRESS.equals(toStatus)) {
          curreStatus.setCompletionDate(null);
          status = toStatus;
        }
        if (TaskStatus.TODO.equals(toStatus)) {
          curreStatus.setCompletionDate(null);
          status = toStatus;
        }

      default:
        break;
    }
    curreStatus.setTaskStatus(status);
    return curreStatus;
  }

  @Override
  @Caching(
      evict = {
        @CacheEvict(
            cacheNames = "tasksByProject",
            key = "@currentUserService.getCurrentUserId() + ':' + #projectId"),
        @CacheEvict(
            cacheNames = "projectById",
            key = "@currentUserService.getCurrentUserId() + ':' + #projectId"),
        @CacheEvict(cacheNames = "userProjects", key = "@currentUserService.getCurrentUserId()")
      })
  public void deleteTask(long projectId, long taskId) {
    long userId = getCurrentUserId();
    log.debug("Deleting taskId={} from projectId={} for userId={}", taskId, projectId, userId);

    var project =
        projectRepository
            .findByIdAndUserId(projectId, userId)
            .orElseThrow(() -> new UnauthorizedException("Invalid Project"));

    TaskEntity taskEntity =
        taskRepository
            .findByIdAndProjectIdAndUserId(taskId, project.getId(), project.getUserId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Task not found for project %d".formatted(projectId)));

    taskRepository.delete(taskEntity);
    log.info("Task deleted successfully with taskId={} from projectId={}", taskId, projectId);
  }

  @Override
  @Caching(
      evict = {
        @CacheEvict(
            cacheNames = "tasksByProject",
            key = "@currentUserService.getCurrentUserId() + ':' + #projectId"),
        @CacheEvict(
            cacheNames = "projectById",
            key = "@currentUserService.getCurrentUserId() + ':' + #projectId"),
        @CacheEvict(cacheNames = "userProjects", key = "@currentUserService.getCurrentUserId()")
      })
  public TaskResponse assignUserTask(long taskId, long userId, long projectId) {

    long projectOwnerId = getCurrentUserId();
    log.debug("Assigning taskId={} in projectId={} to userId={}", taskId, projectId, userId);

    TaskEntity task =
        taskRepository
            .findByIdAndProjectIdAndUserId(taskId, projectId, projectOwnerId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Project not found for projectId %d".formatted(projectId)));

    task.setAssignedUserid(userId);

    log.info("Task assignment updated for taskId={} with assignedUserId={}", taskId, userId);

    return null;
  }
}
