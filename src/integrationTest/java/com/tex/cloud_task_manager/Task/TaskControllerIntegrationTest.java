package com.tex.cloud_task_manager.Task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tex.cloud_task_manager.AbstractWebIntegrationTest;
import com.tex.cloud_task_manager.Project.ProjectEntity;
import com.tex.cloud_task_manager.Project.ProjectRepository;
import com.tex.cloud_task_manager.Project.ProjectStatus;
import com.tex.cloud_task_manager.RefreshToken.RefreshTokenRepository;
import com.tex.cloud_task_manager.User.UserEntity;
import com.tex.cloud_task_manager.User.UserEntityRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class TaskControllerIntegrationTest extends AbstractWebIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private UserEntityRepository userEntityRepository;

  @Autowired private ProjectRepository projectRepository;

  @Autowired private TaskRepository taskRepository;

  @Autowired private RefreshTokenRepository refreshTokenRepository;

  private UserEntity owner;
  private UserEntity otherUser;

  private ProjectEntity ownerProject;
  private ProjectEntity secondOwnerProject;
  private ProjectEntity otherUserProject;

  @BeforeEach
  void setUp() throws Exception {
    taskRepository.deleteAll();
    projectRepository.deleteAll();
    refreshTokenRepository.deleteAll();
    userEntityRepository.deleteAll();

    registerUser("Kevin", "kevin@test.com", "Password123!");
    registerUser("Other User", "other@test.com", "Password123!");

    owner = userEntityRepository.findByEmail("kevin@test.com").orElseThrow();
    otherUser = userEntityRepository.findByEmail("other@test.com").orElseThrow();

    ownerProject = createProject("Owner Project", "Project owned by Kevin", owner.getId());

    secondOwnerProject =
        createProject("Second Owner Project", "Another project owned by Kevin", owner.getId());

    otherUserProject =
        createProject("Other User Project", "Project owned by another user", otherUser.getId());
  }

  @Test
  void createShouldReturn201AndPersistTaskWhenAuthenticatedUserOwnsProject() throws Exception {
    // Why: proves real authenticated HTTP request can create a task under owned project.
    String token = loginAndExtractAccessToken("kevin@test.com", "Password123!");

    mockMvc
        .perform(
            post("/api/task/create")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "title": "Create Task API",
                                  "description": "Build task endpoint",
                                  "projectId": %d
                                }
                                """
                        .formatted(ownerProject.getId())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.projectId").value(ownerProject.getId()))
        .andExpect(jsonPath("$.title").value("Create Task API"))
        .andExpect(jsonPath("$.description").value("Build task endpoint"))
        .andExpect(jsonPath("$.taskStatus").value("TODO"))
        .andExpect(jsonPath("$.priority").value("LOW"));

    TaskEntity savedTask = taskRepository.findAll().stream().findFirst().orElseThrow();

    assertThat(savedTask.getProject().getId()).isEqualTo(ownerProject.getId());
    assertThat(savedTask.getUserId()).isEqualTo(owner.getId());
    assertThat(savedTask.getTitle()).isEqualTo("Create Task API");
    assertThat(savedTask.getDescription()).isEqualTo("Build task endpoint");
    assertThat(savedTask.getTaskStatus()).isEqualTo(TaskStatus.TODO);
    assertThat(savedTask.getPriority()).isEqualTo(TaskPriority.LOW);
    assertThat(savedTask.getCreatedAt()).isNotNull();
  }

  @Test
  void createShouldReturn404AndNotPersistTaskWhenAuthenticatedUserDoesNotOwnProject()
      throws Exception {
    // Why: proves user cannot create task inside another user's project.
    String token = loginAndExtractAccessToken("kevin@test.com", "Password123!");

    mockMvc
        .perform(
            post("/api/task/create")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "title": "Illegal task",
                                  "description": "Should not save",
                                  "projectId": %d
                                }
                                """
                        .formatted(otherUserProject.getId())))
        .andExpect(status().isNotFound())
        .andExpect(
            jsonPath("$.message")
                .value("Project not found for projectId " + otherUserProject.getId()));

    assertThat(taskRepository.findAll()).isEmpty();
  }

  @Test
  void getTasksShouldReturnOnlyTasksForRequestedProjectOwnedByAuthenticatedUser() throws Exception {
    // Why: proves task list does not leak tasks from another owned project or another user.
    String token = loginAndExtractAccessToken("kevin@test.com", "Password123!");

    TaskEntity expectedTask =
        createTask(
            "Task from requested project",
            "Should be returned",
            ownerProject.getId(),
            owner.getId());

    createTask(
        "Task from second owner project",
        "Should not be returned",
        secondOwnerProject.getId(),
        owner.getId());

    createTask(
        "Task from other user project",
        "Should not be returned",
        otherUserProject.getId(),
        otherUser.getId());

    mockMvc
        .perform(
            get("/api/task/project/{projectId}", ownerProject.getId())
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(expectedTask.getId()))
        .andExpect(jsonPath("$[0].projectId").value(ownerProject.getId()))
        .andExpect(jsonPath("$[0].title").value("Task from requested project"));
  }

  @Test
  void getTasksShouldReturn404WhenProjectBelongsToAnotherUser() throws Exception {
    // Why: proves authenticated user cannot list another user's project tasks.
    String token = loginAndExtractAccessToken("kevin@test.com", "Password123!");

    createTask(
        "Other user task", "Should not be visible", otherUserProject.getId(), otherUser.getId());

    mockMvc
        .perform(
            get("/api/task/project/{projectId}", otherUserProject.getId())
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound())
        .andExpect(
            jsonPath("$.message")
                .value("Project not found for projectId " + otherUserProject.getId()));
  }

  @Test
  void updateShouldReturn200AndPersistChangesWhenTaskBelongsToAuthenticatedUserProject()
      throws Exception {
    // Why: proves update works through HTTP only when task belongs to current user's project.
    String token = loginAndExtractAccessToken("kevin@test.com", "Password123!");

    TaskEntity task =
        createTask("Old title", "Old description", ownerProject.getId(), owner.getId());
    task.setTaskStatus(TaskStatus.IN_PROGRESS);
    taskRepository.save(task);

    mockMvc
        .perform(
            put("/api/task/update")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "id": %d,
                                  "projectId": %d,
                                  "title": "Updated title",
                                  "description": "Updated description",
                                  "taskStatus": "DONE",
                                  "dueDate": "2026-06-01",
                                  "completionDate": "2026-06-02",
                                  "priority": "HIGH"
                                }
                                """
                        .formatted(task.getId(), ownerProject.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(task.getId()))
        .andExpect(jsonPath("$.projectId").value(ownerProject.getId()))
        .andExpect(jsonPath("$.title").value("Updated title"))
        .andExpect(jsonPath("$.description").value("Updated description"))
        .andExpect(jsonPath("$.taskStatus").value("DONE"));

    TaskEntity updatedTask = taskRepository.findById(task.getId()).orElseThrow();

    assertThat(updatedTask.getTitle()).isEqualTo("Updated title");
    assertThat(updatedTask.getDescription()).isEqualTo("Updated description");
    assertThat(updatedTask.getTaskStatus()).isEqualTo(TaskStatus.DONE);
    assertThat(updatedTask.getDueDate()).isEqualTo(LocalDate.parse("2026-06-01"));
    assertThat(updatedTask.getCompletionDate()).isEqualTo(LocalDate.parse("2026-06-02"));
  }

  @Test
  void updateShouldSetCompletionDateWhenMovingInProgressTaskToDone() throws Exception {
    String token = loginAndExtractAccessToken("kevin@test.com", "Password123!");

    TaskEntity task = createTask("Finish me", "Move to done", ownerProject.getId(), owner.getId());
    task.setTaskStatus(TaskStatus.IN_PROGRESS);
    taskRepository.save(task);

    mockMvc
        .perform(
            put("/api/task/update")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "id": %d,
                                  "projectId": %d,
                                  "taskStatus": "DONE"
                                }
                                """
                        .formatted(task.getId(), ownerProject.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(task.getId()))
        .andExpect(jsonPath("$.projectId").value(ownerProject.getId()))
        .andExpect(jsonPath("$.taskStatus").value("DONE"))
        .andExpect(jsonPath("$.completionDate").isNotEmpty());

    TaskEntity updatedTask = taskRepository.findById(task.getId()).orElseThrow();

    assertThat(updatedTask.getTaskStatus()).isEqualTo(TaskStatus.DONE);
    assertThat(updatedTask.getCompletionDate()).isNotNull();
  }

  @Test
  void
      updateShouldReturn404AndLeaveTaskUnchangedWhenTaskBelongsToDifferentProjectEvenIfUserOwnsBoth()
          throws Exception {
    // Why: proves user cannot update Project B task through Project A request.
    String token = loginAndExtractAccessToken("kevin@test.com", "Password123!");

    TaskEntity taskInSecondProject =
        createTask(
            "Task in second project",
            "Should not update through first project",
            secondOwnerProject.getId(),
            owner.getId());

    mockMvc
        .perform(
            put("/api/task/update")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "id": %d,
                                  "projectId": %d,
                                  "title": "Cross-project update",
                                  "description": "Should fail",
                                  "taskStatus": "DONE",
                                  "dueDate": "2026-06-01",
                                  "completionDate": "2026-06-02",
                                  "priority": "HIGH"
                                }
                                """
                        .formatted(taskInSecondProject.getId(), ownerProject.getId())))
        .andExpect(status().isNotFound())
        .andExpect(
            jsonPath("$.message").value("Task not found for project " + ownerProject.getId()));

    TaskEntity unchangedTask = taskRepository.findById(taskInSecondProject.getId()).orElseThrow();

    assertThat(unchangedTask.getTitle()).isEqualTo("Task in second project");
    assertThat(unchangedTask.getTaskStatus()).isEqualTo(TaskStatus.TODO);
  }

  @Test
  void updateShouldReturn404AndLeaveTaskUnchangedWhenProjectBelongsToAnotherUser()
      throws Exception {
    // Why: proves project ownership check blocks update before task can change.
    String token = loginAndExtractAccessToken("kevin@test.com", "Password123!");

    TaskEntity otherUserTask =
        createTask(
            "Other user task", "Should not update", otherUserProject.getId(), otherUser.getId());

    mockMvc
        .perform(
            put("/api/task/update")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "id": %d,
                                  "projectId": %d,
                                  "title": "Illegal update",
                                  "description": "Should fail",
                                  "taskStatus": "DONE",
                                  "dueDate": "2026-06-01",
                                  "completionDate": "2026-06-02",
                                  "priority": "HIGH"
                                }
                                """
                        .formatted(otherUserTask.getId(), otherUserProject.getId())))
        .andExpect(status().isNotFound())
        .andExpect(
            jsonPath("$.message")
                .value("Project not found for projectId " + otherUserProject.getId()));

    TaskEntity unchangedTask = taskRepository.findById(otherUserTask.getId()).orElseThrow();

    assertThat(unchangedTask.getTitle()).isEqualTo("Other user task");
    assertThat(unchangedTask.getTaskStatus()).isEqualTo(TaskStatus.TODO);
  }

  @Test
  void deleteShouldReturn204AndRemoveTaskWhenTaskBelongsToAuthenticatedUserProject()
      throws Exception {
    // Why: proves DELETE route passes taskId/projectId correctly and removes scoped task.
    String token = loginAndExtractAccessToken("kevin@test.com", "Password123!");

    TaskEntity task =
        createTask("Delete me", "Should be deleted", ownerProject.getId(), owner.getId());

    mockMvc
        .perform(
            delete("/api/task/{taskId}/project/{projectId}", task.getId(), ownerProject.getId())
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));

    assertThat(taskRepository.findById(task.getId())).isEmpty();
  }

  @Test
  void deleteShouldReturn404AndKeepTaskWhenTaskBelongsToDifferentProjectEvenIfUserOwnsBoth()
      throws Exception {
    // Why: proves user cannot delete Project B task through Project A request.
    String token = loginAndExtractAccessToken("kevin@test.com", "Password123!");

    TaskEntity taskInSecondProject =
        createTask(
            "Task in second project",
            "Should not delete through first project",
            secondOwnerProject.getId(),
            owner.getId());

    mockMvc
        .perform(
            delete(
                    "/api/task/{taskId}/project/{projectId}",
                    taskInSecondProject.getId(),
                    ownerProject.getId())
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound())
        .andExpect(
            jsonPath("$.message").value("Task not found for project " + ownerProject.getId()));

    assertThat(taskRepository.findById(taskInSecondProject.getId())).isPresent();
  }

  @Test
  void deleteShouldReturn401AndKeepTaskWhenProjectBelongsToAnotherUser() throws Exception {
    // Why: proves user cannot delete task through another user's project.
    String token = loginAndExtractAccessToken("kevin@test.com", "Password123!");

    TaskEntity otherUserTask =
        createTask(
            "Other user task", "Should not delete", otherUserProject.getId(), otherUser.getId());

    mockMvc
        .perform(
            delete(
                    "/api/task/{taskId}/project/{projectId}",
                    otherUserTask.getId(),
                    otherUserProject.getId())
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid Project"));

    assertThat(taskRepository.findById(otherUserTask.getId())).isPresent();
  }

  private void registerUser(String name, String email, String password) throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "name": "%s",
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """
                        .formatted(name, email, password)))
        .andExpect(status().isOk());
  }

  private String loginAndExtractAccessToken(String email, String password) throws Exception {
    var result =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """
                            .formatted(email, password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andReturn();

    return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
  }

  private ProjectEntity createProject(String name, String description, Long userId) {
    return projectRepository.save(
        ProjectEntity.builder()
            .name(name)
            .description(description)
            .userId(userId)
            .status(ProjectStatus.ACTIVE)
            .createdAt(LocalDateTime.now())
            .build());
  }

  private TaskEntity createTask(String title, String description, Long projectId, Long userId) {
    ProjectEntity project = projectRepository.findById(projectId).orElseThrow();

    return taskRepository.save(
        TaskEntity.builder()
            .title(title)
            .description(description)
            .project(project)
            .userId(userId)
            .priority(TaskPriority.LOW)
            .taskStatus(TaskStatus.TODO)
            .createdAt(LocalDateTime.now())
            .build());
  }
}
