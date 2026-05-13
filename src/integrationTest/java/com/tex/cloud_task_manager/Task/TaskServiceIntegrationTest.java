package com.tex.cloud_task_manager.Task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.tex.cloud_task_manager.AbstractIntegrationTest;
import com.tex.cloud_task_manager.Project.ProjectEntity;
import com.tex.cloud_task_manager.Project.ProjectPriority;
import com.tex.cloud_task_manager.Project.ProjectRepository;
import com.tex.cloud_task_manager.Project.ProjectStatus;
import com.tex.cloud_task_manager.Security.CurrentUserService;
import com.tex.cloud_task_manager.Task.response_request.TaskResponse;
import com.tex.cloud_task_manager.Task.service.TaskService;
import com.tex.cloud_task_manager.User.UserEntity;
import com.tex.cloud_task_manager.User.UserEntityRepository;
import com.tex.cloud_task_manager.common.exception.ResourceNotFoundException;
import com.tex.cloud_task_manager.common.exception.UnauthorizedException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class TaskServiceIntegrationTest extends AbstractIntegrationTest {

  @Autowired private TaskService taskService;

  @Autowired private TaskRepository taskRepository;

  @Autowired private ProjectRepository projectRepository;

  @Autowired private UserEntityRepository userEntityRepository;

  @MockitoBean private CurrentUserService currentUserService;

  private UserEntity owner;
  private UserEntity otherUser;

  private ProjectEntity ownerProject;
  private ProjectEntity secondOwnerProject;
  private ProjectEntity otherUserProject;

  @BeforeEach
  void setUp() {
    taskRepository.deleteAll();
    projectRepository.deleteAll();
    userEntityRepository.deleteAll();

    owner =
        userEntityRepository.save(
            UserEntity.builder()
                .name("Kevin")
                .email("kevin@test.com")
                .password("encoded-password")
                .createdAt(LocalDateTime.now())
                .build());

    otherUser =
        userEntityRepository.save(
            UserEntity.builder()
                .name("Other User")
                .email("other@test.com")
                .password("encoded-password")
                .createdAt(LocalDateTime.now())
                .build());

    ownerProject =
        projectRepository.save(
            ProjectEntity.builder()
                .name("Owner Project")
                .description("Project owned by Kevin")
                .userId(owner.getId())
                .createdAt(LocalDateTime.now())
                .priority(ProjectPriority.LOW)
                .status(ProjectStatus.ACTIVE)
                .build());

    secondOwnerProject =
        projectRepository.save(
            ProjectEntity.builder()
                .name("Second Owner Project")
                .description("Another project owned by Kevin")
                .userId(owner.getId())
                .status(ProjectStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .priority(ProjectPriority.LOW)
                .build());

    otherUserProject =
        projectRepository.save(
            ProjectEntity.builder()
                .name("Other User Project")
                .description("Project owned by another user")
                .userId(otherUser.getId())
                .status(ProjectStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .priority(ProjectPriority.LOW)
                .build());
  }

  @Test
  void createShouldPersistTaskWithCurrentUserProjectAndDefaultStateWhenProjectBelongsToUser() {
    // Why: proves create stores the ownership fields that update/delete depend on later.
    when(currentUserService.getCurrentUserId()).thenReturn(owner.getId());

    TaskResponse response =
        taskService.create(
            "Create task service", "Implement task service logic", ownerProject.getId());

    TaskEntity savedTask = taskRepository.findById(response.id()).get();

    //   assertThat(savedTask.getProjectId()).isEqualTo(ownerProject.getId());
    assertThat(savedTask.getUserId()).isEqualTo(owner.getId());
    assertThat(savedTask.getTitle()).isEqualTo("Create task service");
    assertThat(savedTask.getDescription()).isEqualTo("Implement task service logic");
    assertThat(savedTask.getPriority()).isEqualTo(TaskPriority.LOW);
    assertThat(savedTask.getTaskStatus()).isEqualTo(TaskStatus.TODO);
    assertThat(savedTask.getCreatedAt()).isNotNull();
  }

  @Test
  void createShouldThrowUnauthorizedAndNotPersistTaskWhenProjectBelongsToAnotherUser() {
    // Why: proves user cannot create a task inside another user's project.
    when(currentUserService.getCurrentUserId()).thenReturn(owner.getId());

    assertThatThrownBy(
            () ->
                taskService.create(
                    "Illegal task", "Should not be created", otherUserProject.getId()))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Project not found for projectId " + otherUserProject.getId());

    assertThat(taskRepository.findAll()).isEmpty();
  }

  @Test
  void getTasksShouldReturnOnlyTasksForRequestedProjectAndCurrentUser() {
    // Why: proves list endpoint does not leak tasks from another project or another user.
    when(currentUserService.getCurrentUserId()).thenReturn(owner.getId());

    TaskEntity expectedTask =
        taskRepository.save(
            TaskEntity.builder()
                .title("Task from requested project")
                .description("Should be returned")
                //    .projectId(ownerProject.getId())
                .userId(owner.getId())
                .createdAt(LocalDateTime.now())
                .priority(TaskPriority.LOW)
                .taskStatus(TaskStatus.TODO)
                .project(ownerProject)
                .build());

    taskRepository.save(
        TaskEntity.builder()
            .title("Task from second owner project")
            .description("Should not be returned")
            // .projectId(secondOwnerProject.getId())
            .userId(owner.getId())
            .createdAt(LocalDateTime.now())
            .priority(TaskPriority.LOW)
            .taskStatus(TaskStatus.TODO)
            .project(secondOwnerProject)
            .build());

    taskRepository.save(
        TaskEntity.builder()
            .title("Task from other user project")
            .description("Should not be returned")
            // .projectId(otherUserProject.getId())
            .createdAt(LocalDateTime.now())
            .userId(otherUser.getId())
            .priority(TaskPriority.LOW)
            .taskStatus(TaskStatus.TODO)
            .project(otherUserProject)
            .build());

    List<TaskResponse> response = taskService.getTasks(ownerProject.getId());

    assertThat(response).hasSize(1);
    assertThat(response.get(0).id()).isEqualTo(expectedTask.getId());
    assertThat(response.get(0).projectId()).isEqualTo(ownerProject.getId());
    assertThat(response.get(0).title()).isEqualTo("Task from requested project");
  }

  @Test
  void getTasksShouldThrowUnauthorizedWhenProjectBelongsToAnotherUser() {
    // Why: proves project ownership is checked before returning task data.
    when(currentUserService.getCurrentUserId()).thenReturn(owner.getId());

    taskRepository.save(
        TaskEntity.builder()
            .project(otherUserProject)
            .title("Other user's task")
            .description("Should not be visible")
            //   .projectId(otherUserProject.getId())
            .userId(otherUser.getId())
            .createdAt(LocalDateTime.now())
            .priority(TaskPriority.LOW)
            .taskStatus(TaskStatus.TODO)
            .build());

    assertThatThrownBy(() -> taskService.getTasks(otherUserProject.getId()))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void updateTaskShouldPersistChangesWhenTaskBelongsToRequestedProjectAndCurrentUser() {
    // Why: proves real DB update works when taskId + projectId + userId all match.
    when(currentUserService.getCurrentUserId()).thenReturn(owner.getId());

    TaskEntity task =
        taskRepository.save(
            TaskEntity.builder()
                .title("Old title")
                .description("Old description")
                //  .projectId(ownerProject.getId())
                .userId(owner.getId())
                .project(ownerProject)
                .createdAt(LocalDateTime.now())
                .priority(TaskPriority.LOW)
                .taskStatus(TaskStatus.IN_PROGRESS)
                .build());

    TaskResponse response =
        taskService.updateTask(
            task.getId(),
            ownerProject.getId(),
            "Updated title",
            "Updated description",
            "DONE",
            "2026-06-01",
            "2026-06-02",
            "LOW");

    TaskEntity updatedTask = taskRepository.findById(task.getId()).orElseThrow();

    assertThat(response.id()).isEqualTo(task.getId());
    assertThat(updatedTask.getTitle()).isEqualTo("Updated title");
    assertThat(updatedTask.getDescription()).isEqualTo("Updated description");
    assertThat(updatedTask.getTaskStatus()).isEqualTo(TaskStatus.DONE);
    assertThat(updatedTask.getDueDate()).isEqualTo(LocalDate.parse("2026-06-01"));
    assertThat(updatedTask.getCompletionDate()).isEqualTo(LocalDate.parse("2026-06-02"));
  }

  @Test
  void updateTaskShouldThrowUnauthorizedWhenProjectBelongsToAnotherUser() {
    // Why: proves update stops at project ownership failure before task can be changed.
    when(currentUserService.getCurrentUserId()).thenReturn(owner.getId());

    TaskEntity otherUserTask =
        taskRepository.save(
            TaskEntity.builder()
                .title("Other user task")
                .description("Should not update")
                //    .projectId(otherUserProject.getId())
                .project(otherUserProject)
                .userId(otherUser.getId())
                .createdAt(LocalDateTime.now())
                .priority(TaskPriority.LOW)
                .taskStatus(TaskStatus.TODO)
                .build());

    assertThatThrownBy(
            () ->
                taskService.updateTask(
                    otherUserTask.getId(),
                    otherUserProject.getId(),
                    "Illegal update",
                    "Should fail",
                    "DONE",
                    "2026-06-01",
                    "2026-06-02",
                    "LOW"))
        .isInstanceOf(ResourceNotFoundException.class);

    TaskEntity unchangedTask = taskRepository.findById(otherUserTask.getId()).orElseThrow();

    assertThat(unchangedTask.getTitle()).isEqualTo("Other user task");
    assertThat(unchangedTask.getTaskStatus()).isEqualTo(TaskStatus.TODO);
  }

  @Test
  void
      updateTaskShouldThrowResourceNotFoundWhenTaskBelongsToDifferentProjectEvenIfUserOwnsBothProjects() {
    // Why: proves user cannot update a Project B task through a Project A route.
    when(currentUserService.getCurrentUserId()).thenReturn(owner.getId());

    TaskEntity taskInSecondProject =
        taskRepository.save(
            TaskEntity.builder()
                .title("Task in second project")
                .description("Should not update through first project")
                //  .projectId(secondOwnerProject.getId())
                .project(secondOwnerProject)
                .userId(owner.getId())
                .createdAt(LocalDateTime.now())
                .priority(TaskPriority.LOW)
                .taskStatus(TaskStatus.TODO)
                .build());

    assertThatThrownBy(
            () ->
                taskService.updateTask(
                    taskInSecondProject.getId(),
                    ownerProject.getId(),
                    "Cross-project update",
                    "Should fail",
                    "DONE",
                    "2026-06-01",
                    "2026-06-02",
                    "LOW"))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Task not found for project " + ownerProject.getId());

    TaskEntity unchangedTask = taskRepository.findById(taskInSecondProject.getId()).orElseThrow();

    assertThat(unchangedTask.getTitle()).isEqualTo("Task in second project");
    assertThat(unchangedTask.getTaskStatus()).isEqualTo(TaskStatus.TODO);
  }

  @Test
  void updateTaskShouldThrowResourceNotFoundWhenTaskBelongsToDifferentUserEvenIfProjectIdMatches() {
    // Why: proves task user scope is enforced, not only projectId.
    when(currentUserService.getCurrentUserId()).thenReturn(owner.getId());

    TaskEntity otherUserTask =
        taskRepository.save(
            TaskEntity.builder()
                .title("Wrong user task")
                .description("Should not update")
                .project(otherUserProject)
                .userId(otherUser.getId())
                .createdAt(LocalDateTime.now())
                .priority(TaskPriority.LOW)
                .taskStatus(TaskStatus.TODO)
                .build());

    assertThatThrownBy(
            () ->
                taskService.updateTask(
                    otherUserTask.getId(),
                    ownerProject.getId(),
                    "Illegal update",
                    "Should fail",
                    "DONE",
                    "2026-06-01",
                    "2026-06-02",
                    "LOW"))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Task not found for project " + ownerProject.getId());

    TaskEntity unchangedTask = taskRepository.findById(otherUserTask.getId()).orElseThrow();

    assertThat(unchangedTask.getTitle()).isEqualTo("Wrong user task");
    assertThat(unchangedTask.getTaskStatus()).isEqualTo(TaskStatus.TODO);
  }

  @Test
  void deleteTaskShouldRemoveTaskWhenTaskBelongsToRequestedProjectAndCurrentUser() {
    // Why: proves delete removes the exact scoped task from the real DB.
    when(currentUserService.getCurrentUserId()).thenReturn(owner.getId());

    TaskEntity task =
        taskRepository.save(
            TaskEntity.builder()
                .title("Delete me")
                .description("Should be deleted")
                .project(ownerProject)
                .userId(owner.getId())
                .createdAt(LocalDateTime.now())
                .priority(TaskPriority.LOW)
                .taskStatus(TaskStatus.TODO)
                .build());

    taskService.deleteTask(ownerProject.getId(), task.getId());

    assertThat(taskRepository.findById(task.getId())).isEmpty();
  }

  @Test
  void deleteTaskShouldThrowUnauthorizedWhenProjectBelongsToAnotherUser() {
    // Why: proves another user's project blocks delete before task can be removed.
    when(currentUserService.getCurrentUserId()).thenReturn(owner.getId());

    TaskEntity otherUserTask =
        taskRepository.save(
            TaskEntity.builder()
                .title("Other user task")
                .description("Should not delete")
                .project(otherUserProject)
                .userId(otherUser.getId())
                .createdAt(LocalDateTime.now())
                .priority(TaskPriority.LOW)
                .taskStatus(TaskStatus.TODO)
                .build());

    assertThatThrownBy(
            () -> taskService.deleteTask(otherUserProject.getId(), otherUserTask.getId()))
        .isInstanceOf(UnauthorizedException.class)
        .hasMessage("Invalid Project");

    assertThat(taskRepository.findById(otherUserTask.getId())).isPresent();
  }

  @Test
  void
      deleteTaskShouldThrowResourceNotFoundWhenTaskBelongsToDifferentProjectEvenIfUserOwnsBothProjects() {
    // Why: proves user cannot delete a Project B task through a Project A route.
    when(currentUserService.getCurrentUserId()).thenReturn(owner.getId());

    TaskEntity taskInSecondProject =
        taskRepository.save(
            TaskEntity.builder()
                .title("Task in second project")
                .description("Should not delete through first project")
                //  .projectId(secondOwnerProject.getId())
                .project(secondOwnerProject)
                .userId(owner.getId())
                .createdAt(LocalDateTime.now())
                .priority(TaskPriority.LOW)
                .taskStatus(TaskStatus.TODO)
                .build());

    assertThatThrownBy(
            () -> taskService.deleteTask(ownerProject.getId(), taskInSecondProject.getId()))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Task not found for project " + ownerProject.getId());

    assertThat(taskRepository.findById(taskInSecondProject.getId())).isPresent();
  }

  @Test
  void deleteTaskShouldThrowResourceNotFoundWhenTaskBelongsToDifferentUserEvenIfProjectIdMatches() {
    // Why: proves delete requires userId match, not just taskId and projectId.
    when(currentUserService.getCurrentUserId()).thenReturn(owner.getId());

    TaskEntity otherUserTask =
        taskRepository.save(
            TaskEntity.builder()
                .title("Wrong user task")
                .description("Should not delete")
                .project(ownerProject)
                .userId(otherUser.getId())
                .createdAt(LocalDateTime.now())
                .priority(TaskPriority.LOW)
                .taskStatus(TaskStatus.TODO)
                .build());

    assertThatThrownBy(() -> taskService.deleteTask(ownerProject.getId(), otherUserTask.getId()))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Task not found for project " + ownerProject.getId());

    assertThat(taskRepository.findById(otherUserTask.getId())).isPresent();
  }
}
