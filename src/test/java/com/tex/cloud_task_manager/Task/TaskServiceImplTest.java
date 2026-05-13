package com.tex.cloud_task_manager.Task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tex.cloud_task_manager.Project.ProjectEntity;
import com.tex.cloud_task_manager.Project.ProjectRepository;
import com.tex.cloud_task_manager.Security.CurrentUserService;
import com.tex.cloud_task_manager.Task.response_request.TaskResponse;
import com.tex.cloud_task_manager.Task.service.TaskServiceImpl;
import com.tex.cloud_task_manager.common.exception.ResourceNotFoundException;
import com.tex.cloud_task_manager.common.exception.UnauthorizedException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

  @Mock private TaskRepository taskRepository;

  @Mock private ProjectRepository projectRepository;

  @Mock private CurrentUserService currentUserService;

  @InjectMocks private TaskServiceImpl taskService;

  private final long userId = 100L;
  private final long projectId = 10L;
  private final long taskId = 50L;

  private ProjectEntity ownedProject;

  @BeforeEach
  void setUp() {
    ownedProject =
        ProjectEntity.builder()
            .id(projectId)
            .userId(userId)
            .name("Owned Project")
            .description("Project owned by current user")
            .build();
  }

  @Test
  void createShouldSaveTaskWithCorrectOwnershipAndDefaultsWhenProjectBelongsToUser() {
    when(currentUserService.getCurrentUserId()).thenReturn(userId);
    when(projectRepository.findByIdAndUserId(projectId, userId))
        .thenReturn(Optional.of(ownedProject));

    TaskEntity savedTask =
        TaskEntity.builder()
            .id(taskId)
            .project(ownedProject)
            .userId(userId)
            .title("Create Task API")
            .description("Build task endpoints")
            .priority(TaskPriority.LOW)
            .taskStatus(TaskStatus.TODO)
            .build();

    when(taskRepository.save(any(TaskEntity.class))).thenReturn(savedTask);

    TaskResponse response =
        taskService.create("Create Task API", "Build task endpoints", projectId);

    assertEquals(taskId, response.id());
    assertEquals(projectId, response.projectId());
    assertEquals("Create Task API", response.title());
    assertEquals(TaskStatus.TODO, response.taskStatus());
    assertEquals(TaskPriority.LOW, response.priority());

    ArgumentCaptor<TaskEntity> captor = ArgumentCaptor.forClass(TaskEntity.class);
    verify(taskRepository).save(captor.capture());

    TaskEntity taskToSave = captor.getValue();

    assertEquals(ownedProject, taskToSave.getProject());
    assertEquals(userId, taskToSave.getUserId());
    assertEquals(TaskStatus.TODO, taskToSave.getTaskStatus());
    assertEquals(TaskPriority.LOW, taskToSave.getPriority());
    assertNotNull(taskToSave.getCreatedAt());

    verify(projectRepository).findByIdAndUserId(projectId, userId);
  }

  @Test
  void createShouldThrowUnauthorizedAndNotSaveWhenProjectDoesNotBelongToUser() {
    when(currentUserService.getCurrentUserId()).thenReturn(userId);
    when(projectRepository.findByIdAndUserId(projectId, userId)).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> taskService.create("Illegal Task", "Should not save", projectId));

    verify(taskRepository, never()).save(any());
  }

  @Test
  void getTasksShouldReturnTasksForOwnedProjectOnlyThroughScopedRepositoryCall() {
    when(currentUserService.getCurrentUserId()).thenReturn(userId);
    when(projectRepository.findByIdAndUserId(projectId, userId))
        .thenReturn(Optional.of(ownedProject));

    TaskEntity taskOne =
        TaskEntity.builder()
            .id(1L)
            .project(ownedProject)
            .userId(userId)
            .title("Task One")
            .description("First task")
            .priority(TaskPriority.LOW)
            .taskStatus(TaskStatus.TODO)
            .build();

    TaskEntity taskTwo =
        TaskEntity.builder()
            .id(2L)
            .project(ownedProject)
            .userId(userId)
            .title("Task Two")
            .description("Second task")
            .priority(TaskPriority.LOW)
            .taskStatus(TaskStatus.IN_PROGRESS)
            .build();

    when(taskRepository.findByProjectIdAndUserId(projectId, userId))
        .thenReturn(List.of(taskOne, taskTwo));

    List<TaskResponse> response = taskService.getTasks(projectId);

    assertEquals(2, response.size());
    assertTrue(response.stream().allMatch(task -> task.projectId().equals(projectId)));

    verify(taskRepository).findByProjectIdAndUserId(projectId, userId);
  }

  @Test
  void getTasksShouldThrowUnauthorizedAndNotQueryTasksWhenProjectIsInvalid() {
    when(currentUserService.getCurrentUserId()).thenReturn(userId);
    when(projectRepository.findByIdAndUserId(projectId, userId)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> taskService.getTasks(projectId));

    verify(taskRepository, never()).findByProjectIdAndUserId(anyLong(), anyLong());
  }

  @Test
  void updateTaskShouldSaveWhenProjectAndTaskBelongToUser() {
    when(currentUserService.getCurrentUserId()).thenReturn(userId);
    when(projectRepository.findByIdAndUserId(projectId, userId))
        .thenReturn(Optional.of(ownedProject));

    TaskEntity existingTask =
        TaskEntity.builder()
            .id(taskId)
            .project(ownedProject)
            .userId(userId)
            .title("Old title")
            .description("Old description")
            .priority(TaskPriority.LOW)
            .taskStatus(TaskStatus.TODO)
            .build();

    when(taskRepository.findByIdAndProjectIdAndUserId(taskId, projectId, userId))
        .thenReturn(Optional.of(existingTask));

    when(taskRepository.save(any(TaskEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    TaskResponse response =
        taskService.updateTask(
            taskId,
            projectId,
            "Updated title",
            "Updated description",
            "IN_PROGRESS",
            "2026-06-01",
            "2026-06-02",
            "LOW");

    assertEquals(taskId, response.id());
    assertEquals(projectId, response.projectId());
    assertEquals("Updated title", response.title());
    assertEquals("Updated description", response.description());
    assertEquals(TaskStatus.IN_PROGRESS, response.taskStatus());
    assertEquals(LocalDate.parse("2026-06-01"), response.dueDate());
    assertEquals(LocalDate.parse("2026-06-02"), response.completionDate());

    verify(taskRepository).findByIdAndProjectIdAndUserId(taskId, projectId, userId);
    verify(taskRepository).save(existingTask);
  }

  @Test
  void updateTaskShouldSetCompletionDateWhenMovingFromInProgressToDone() {
    when(currentUserService.getCurrentUserId()).thenReturn(userId);
    when(projectRepository.findByIdAndUserId(projectId, userId))
        .thenReturn(Optional.of(ownedProject));

    TaskEntity existingTask =
        TaskEntity.builder()
            .id(taskId)
            .project(ownedProject)
            .userId(userId)
            .title("Finish me")
            .description("Move to done")
            .priority(TaskPriority.LOW)
            .taskStatus(TaskStatus.IN_PROGRESS)
            .build();

    when(taskRepository.findByIdAndProjectIdAndUserId(taskId, projectId, userId))
        .thenReturn(Optional.of(existingTask));
    when(taskRepository.save(any(TaskEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    TaskResponse response =
        taskService.updateTask(taskId, projectId, null, null, "DONE", null, null, null);

    assertEquals(TaskStatus.DONE, response.taskStatus());
    assertNotNull(response.completionDate());
    assertEquals(TaskStatus.DONE, existingTask.getTaskStatus());
    assertNotNull(existingTask.getCompletionDate());

    verify(taskRepository).save(existingTask);
  }

  @Test
  void updateTaskShouldClearCompletionDateWhenMovingFromDoneToInProgress() {
    when(currentUserService.getCurrentUserId()).thenReturn(userId);
    when(projectRepository.findByIdAndUserId(projectId, userId))
        .thenReturn(Optional.of(ownedProject));

    TaskEntity existingTask =
        TaskEntity.builder()
            .id(taskId)
            .project(ownedProject)
            .userId(userId)
            .title("Reopen me")
            .description("Move back")
            .priority(TaskPriority.LOW)
            .taskStatus(TaskStatus.DONE)
            .completionDate(LocalDate.of(2026, 5, 13))
            .build();

    when(taskRepository.findByIdAndProjectIdAndUserId(taskId, projectId, userId))
        .thenReturn(Optional.of(existingTask));
    when(taskRepository.save(any(TaskEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    TaskResponse response =
        taskService.updateTask(taskId, projectId, null, null, "IN_PROGRESS", null, null, null);

    assertEquals(TaskStatus.IN_PROGRESS, response.taskStatus());
    assertEquals(null, response.completionDate());
    assertEquals(TaskStatus.IN_PROGRESS, existingTask.getTaskStatus());
    assertEquals(null, existingTask.getCompletionDate());

    verify(taskRepository).save(existingTask);
  }

  @Test
  void updateTaskShouldRejectUnchangedStatus() {
    when(currentUserService.getCurrentUserId()).thenReturn(userId);
    when(projectRepository.findByIdAndUserId(projectId, userId))
        .thenReturn(Optional.of(ownedProject));

    TaskEntity existingTask =
        TaskEntity.builder()
            .id(taskId)
            .project(ownedProject)
            .userId(userId)
            .title("Already todo")
            .description("No status change")
            .priority(TaskPriority.LOW)
            .taskStatus(TaskStatus.TODO)
            .build();

    when(taskRepository.findByIdAndProjectIdAndUserId(taskId, projectId, userId))
        .thenReturn(Optional.of(existingTask));

    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () -> taskService.updateTask(taskId, projectId, null, null, "TODO", null, null, null));

    assertEquals("TaskStatus unchanged", exception.getMessage());
    verify(taskRepository, never()).save(any());
  }

  @Test
  void updateTaskShouldThrowUnauthorizedAndNotTouchTaskWhenProjectIsInvalid() {
    when(currentUserService.getCurrentUserId()).thenReturn(userId);
    when(projectRepository.findByIdAndUserId(projectId, userId)).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () ->
            taskService.updateTask(
                taskId,
                projectId,
                "Updated title",
                "Updated description",
                "DONE",
                "2026-06-01",
                "2026-06-02",
                "LOW"));

    verify(taskRepository, never()).findByIdAndProjectIdAndUserId(anyLong(), anyLong(), anyLong());
    verify(taskRepository, never()).save(any());
  }

  @Test
  void updateTaskShouldThrowResourceNotFoundWhenTaskIsNotScopedToProjectAndUser() {
    when(currentUserService.getCurrentUserId()).thenReturn(userId);
    when(projectRepository.findByIdAndUserId(projectId, userId))
        .thenReturn(Optional.of(ownedProject));

    when(taskRepository.findByIdAndProjectIdAndUserId(taskId, projectId, userId))
        .thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () ->
            taskService.updateTask(
                taskId,
                projectId,
                "Updated title",
                "Updated description",
                "DONE",
                "2026-06-01",
                "2026-06-02",
                "LOW"));

    verify(taskRepository, never()).save(any());
  }

  @Test
  void deleteTaskShouldDeleteWhenProjectAndTaskBelongToUser() {
    when(currentUserService.getCurrentUserId()).thenReturn(userId);
    when(projectRepository.findByIdAndUserId(projectId, userId))
        .thenReturn(Optional.of(ownedProject));

    TaskEntity existingTask =
        TaskEntity.builder()
            .id(taskId)
            .project(ownedProject)
            .userId(userId)
            .title("Delete me")
            .description("Delete this task")
            .priority(TaskPriority.LOW)
            .taskStatus(TaskStatus.TODO)
            .build();

    when(taskRepository.findByIdAndProjectIdAndUserId(taskId, projectId, userId))
        .thenReturn(Optional.of(existingTask));

    taskService.deleteTask(projectId, taskId);

    verify(taskRepository).findByIdAndProjectIdAndUserId(taskId, projectId, userId);
    verify(taskRepository).delete(existingTask);
  }

  @Test
  void deleteTaskShouldThrowUnauthorizedAndNotTouchTaskWhenProjectIsInvalid() {
    when(currentUserService.getCurrentUserId()).thenReturn(userId);
    when(projectRepository.findByIdAndUserId(projectId, userId)).thenReturn(Optional.empty());

    assertThrows(UnauthorizedException.class, () -> taskService.deleteTask(projectId, taskId));

    verify(taskRepository, never()).findByIdAndProjectIdAndUserId(anyLong(), anyLong(), anyLong());
    verify(taskRepository, never()).delete(any());
  }

  @Test
  void deleteTaskShouldThrowResourceNotFoundWhenTaskIsNotScopedToProjectAndUser() {
    when(currentUserService.getCurrentUserId()).thenReturn(userId);
    when(projectRepository.findByIdAndUserId(projectId, userId))
        .thenReturn(Optional.of(ownedProject));

    when(taskRepository.findByIdAndProjectIdAndUserId(taskId, projectId, userId))
        .thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> taskService.deleteTask(projectId, taskId));

    verify(taskRepository, never()).delete(any());
  }
}
