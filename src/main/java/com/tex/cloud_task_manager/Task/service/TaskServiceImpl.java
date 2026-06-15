package com.tex.cloud_task_manager.Task.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

  private final TaskRepository taskRepository;
  private final ProjectRepository projectRepository;
  private final CurrentUserService currentUserService;

  private long getCurrentUserId() {

    return currentUserService.getCurrentUserId();
  }

  @Override
  public TaskResponse create(String title, String description, long projectId) {

    long userId = getCurrentUserId();

    var project = projectRepository
        .findByIdAndUserId(projectId, userId)
        .orElseThrow(
            () -> new ResourceNotFoundException(
                "Project not found for projectId %d".formatted(projectId)));

    TaskEntity taskEntity = TaskEntity.builder()
        .title(title)
        .project(project)
        .userId(userId)
        .description(description)
        .priority(TaskPriority.LOW)
        .taskStatus(TaskStatus.TODO)
        .createdAt(LocalDateTime.now())
        .build();

    return TaskResponse.from(taskRepository.save(taskEntity));
  }

  @Override
  public List<TaskResponse> getTasks(long projectId) {

    var project = projectRepository
        .findByIdAndUserId(projectId, getCurrentUserId())
        .orElseThrow(
            () -> new ResourceNotFoundException(
                "Project not found for projectId %d".formatted(projectId)));

    List<TaskEntity> taskEntity = taskRepository.findByProjectIdAndUserId(projectId, project.getUserId());

    return taskEntity.stream().map(TaskResponse::from).toList();
  }

  @Override
  public TaskResponse updateTask(
      long taskId,
      long projectId,
      String title,
      String description,
      String taskStatus,
      String dueDate,
      String completionDate,
      String priority) {

    var project = projectRepository
        .findByIdAndUserId(projectId, getCurrentUserId())
        .orElseThrow(
            () -> new ResourceNotFoundException(
                "Project not found for projectId %d".formatted(projectId)));

    TaskEntity taskEntity = taskRepository
        .findByIdAndProjectIdAndUserId(taskId, project.getId(), project.getUserId())
        .orElseThrow(
            () -> new ResourceNotFoundException(
                "Task not found for project %d".formatted(projectId)));

    if (description != null && !description.isBlank())
      taskEntity.setDescription(description);
    if (dueDate != null && !dueDate.isBlank())
      taskEntity.setDueDate(LocalDate.parse(dueDate));
    if (title != null && !title.isBlank())
      taskEntity.setTitle(title);
    if (taskStatus != null && !taskStatus.isBlank()
        && !taskEntity.getTaskStatus().equals(TaskStatus.valueOf(taskStatus)))
      taskEntity = updateStatus(taskEntity, TaskStatus.valueOf(taskStatus));
    if (completionDate != null && !completionDate.isBlank())
      taskEntity.setCompletionDate(LocalDate.parse(completionDate));
    if (priority != null && !priority.isBlank() && !taskEntity.getPriority().equals(TaskPriority.valueOf(priority)))
      taskEntity.setPriority(TaskPriority.valueOf(priority));
    if (project != null)
      taskEntity.setProject(project);

    return TaskResponse.from(taskRepository.save(taskEntity));
  }

  private TaskEntity updateStatus(TaskEntity curreStatus, TaskStatus toStatus) {

    TaskStatus status = curreStatus.getTaskStatus();

    switch (status) {
      case TaskStatus.TODO:
        if (TaskStatus.IN_PROGRESS.equals(toStatus))
          status = toStatus;
        else {
          throw new BadRequestException(
              "Unable to move to %s from %s".formatted(TaskStatus.IN_PROGRESS, status));
        }
        break;
      case TaskStatus.IN_PROGRESS:
        if (TaskStatus.DONE.equals(toStatus))
          curreStatus.setCompletionDate(LocalDate.now());
        status = toStatus;
        if (TaskStatus.TODO.equals(toStatus))
          status = toStatus;
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
  public void deleteTask(long projectId, long taskId) {

    var project = projectRepository
        .findByIdAndUserId(projectId, getCurrentUserId())
        .orElseThrow(() -> new UnauthorizedException("Invalid Project"));

    TaskEntity taskEntity = taskRepository
        .findByIdAndProjectIdAndUserId(taskId, project.getId(), project.getUserId())
        .orElseThrow(
            () -> new ResourceNotFoundException(
                "Task not found for project %d".formatted(projectId)));

    taskRepository.delete(taskEntity);
  }

  @Override
  public TaskResponse assignUserTask(long taskId, long userId, long projectId) {

    long projectOwnerId = getCurrentUserId();

    TaskEntity task = taskRepository.findByIdAndProjectIdAndUserId(taskId, projectId, projectOwnerId).orElseThrow(
        () -> new ResourceNotFoundException(
            "Project not found for projectId %d".formatted(projectId)));

    task.setAssignedUserid(userId);

    return null;
  }
}
