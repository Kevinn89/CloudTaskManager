package com.tex.cloud_task_manager.Task;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tex.cloud_task_manager.Config.JwtAuthFilter;
import com.tex.cloud_task_manager.Task.response_request.TaskResponse;
import com.tex.cloud_task_manager.Task.service.TaskService;
import com.tex.cloud_task_manager.common.exception.ResourceNotFoundException;
import com.tex.cloud_task_manager.common.exception.UnauthorizedException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
class TaskControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private TaskService taskService;

  @MockitoBean private JwtAuthFilter jwtAuthFilter;

  @Test
  void createShouldReturn201AndCallServiceWithRequestFields() throws Exception {
    // Why: proves JSON request fields are mapped into TaskService.create().
    TaskResponse response =
        new TaskResponse(
            50L,
            10L,
            1L,
            null,
            "Create task API",
            "Build task endpoint",
            TaskStatus.TODO,
            TaskPriority.LOW,
            null,
            null,
            LocalDateTime.now(),
            null);

    when(taskService.create("Create task API", "Build task endpoint", 10L)).thenReturn(response);

    mockMvc
        .perform(
            post("/api/task/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "title": "Create task API",
                                  "description": "Build task endpoint",
                                  "projectId": 10
                                }
                                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(50))
        .andExpect(jsonPath("$.projectId").value(10))
        .andExpect(jsonPath("$.title").value("Create task API"))
        .andExpect(jsonPath("$.taskStatus").value("TODO"))
        .andExpect(jsonPath("$.priority").value("LOW"));

    verify(taskService).create("Create task API", "Build task endpoint", 10L);
  }

  @Test
  void createShouldReturnUnauthorizedWhenServiceRejectsProjectOwnership() throws Exception {
    // Why: proves controller does not convert an ownership failure into a fake success.
    when(taskService.create("Illegal task", "Should fail", 99L))
        .thenThrow(new UnauthorizedException("Invalid Project"));

    mockMvc
        .perform(
            post("/api/task/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "title": "Illegal task",
                                  "description": "Should fail",
                                  "projectId": 99
                                }
                                """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid Project"));

    verify(taskService).create("Illegal task", "Should fail", 99L);
  }

  @Test
  void getTasksShouldReturnTaskListForProjectIdFromPath() throws Exception {
    // Why: proves projectId comes from the URL and the controller returns the service list.
    TaskResponse taskOne =
        new TaskResponse(
            1L,
            10L,
            null,
            null,
            "Task one",
            "First task",
            TaskStatus.TODO,
            TaskPriority.LOW,
            null,
            null,
            LocalDateTime.now(),
            null);

    TaskResponse taskTwo =
        new TaskResponse(
            2L,
            10L,
            null,
            null,
            "Task two",
            "Second task",
            TaskStatus.IN_PROGRESS,
            TaskPriority.HIGH,
            null,
            null,
            LocalDateTime.now(),
            null);

    when(taskService.getTasks(10L)).thenReturn(List.of(taskOne, taskTwo));

    mockMvc
        .perform(get("/api/task/project/10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].projectId").value(10))
        .andExpect(jsonPath("$[0].title").value("Task one"))
        .andExpect(jsonPath("$[1].id").value(2))
        .andExpect(jsonPath("$[1].title").value("Task two"));

    verify(taskService).getTasks(10L);
  }

  @Test
  void getTasksShouldReturnUnauthorizedWhenServiceRejectsProjectOwnership() throws Exception {
    // Why: proves list endpoint respects service-level project ownership failure.
    when(taskService.getTasks(99L)).thenThrow(new UnauthorizedException("Invalid Project"));

    mockMvc
        .perform(get("/api/task/project/99"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid Project"));

    verify(taskService).getTasks(99L);
  }

  @Test
  void updateTaskShouldReturn200AndCallServiceWithRequestFields() throws Exception {
    // Why: proves every update field is passed to TaskService.updateTask().
    TaskResponse response =
        new TaskResponse(
            50L,
            10L,
            null,
            null,
            "Updated title",
            "Updated description",
            TaskStatus.DONE,
            TaskPriority.HIGH,
            LocalDate.parse("2026-06-01"),
            LocalDate.parse("2026-06-02"),
            LocalDateTime.now(),
            null);

    when(taskService.updateTask(
            50L,
            10L,
            "Updated title",
            "Updated description",
            "DONE",
            "2026-06-01",
            "2026-06-02",
            "HIGH"))
        .thenReturn(response);

    mockMvc
        .perform(
            put("/api/task/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "id": 50,
                                  "projectId": 10,
                                  "title": "Updated title",
                                  "description": "Updated description",
                                  "taskStatus": "DONE",
                                  "dueDate": "2026-06-01",
                                  "completionDate": "2026-06-02",
                                  "priority": "HIGH"
                                }
                                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(50))
        .andExpect(jsonPath("$.projectId").value(10))
        .andExpect(jsonPath("$.title").value("Updated title"))
        .andExpect(jsonPath("$.description").value("Updated description"))
        .andExpect(jsonPath("$.taskStatus").value("DONE"))
        .andExpect(jsonPath("$.priority").value("HIGH"));

    verify(taskService)
        .updateTask(
            50L,
            10L,
            "Updated title",
            "Updated description",
            "DONE",
            "2026-06-01",
            "2026-06-02",
            "HIGH");
  }

  @Test
  void updateTaskShouldReturnDoneWithCompletionDateWhenServiceCompletesTask() throws Exception {
    TaskResponse response =
        new TaskResponse(
            50L,
            10L,
            null,
            null,
            "Finish task",
            "Complete task through controller",
            TaskStatus.DONE,
            TaskPriority.LOW,
            null,
            LocalDate.parse("2026-05-13"),
            LocalDateTime.now(),
            null);

    when(taskService.updateTask(50L, 10L, null, null, "DONE", null, null, null))
        .thenReturn(response);

    mockMvc
        .perform(
            put("/api/task/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "id": 50,
                                  "projectId": 10,
                                  "taskStatus": "DONE"
                                }
                                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(50))
        .andExpect(jsonPath("$.projectId").value(10))
        .andExpect(jsonPath("$.taskStatus").value("DONE"))
        .andExpect(jsonPath("$.completionDate").value("2026-05-13"));

    verify(taskService).updateTask(50L, 10L, null, null, "DONE", null, null, null);
  }

  @Test
  void updateTaskShouldReturnNotFoundWhenTaskIsNotScopedToProjectAndUser() throws Exception {
    // Why: proves controller returns service failure when taskId + projectId + userId does not
    // match.
    when(taskService.updateTask(
            50L, 10L, "Bad update", "Should fail", "DONE", "2026-06-01", "2026-06-02", "HIGH"))
        .thenThrow(new ResourceNotFoundException("Task not found for project 10"));

    mockMvc
        .perform(
            put("/api/task/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "id": 50,
                                  "projectId": 10,
                                  "title": "Bad update",
                                  "description": "Should fail",
                                  "taskStatus": "DONE",
                                  "dueDate": "2026-06-01",
                                  "completionDate": "2026-06-02",
                                  "priority": "HIGH"
                                }
                                """))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Task not found for project 10"));

    verify(taskService)
        .updateTask(
            50L, 10L, "Bad update", "Should fail", "DONE", "2026-06-01", "2026-06-02", "HIGH");
  }

  @Test
  void deleteTaskShouldReturn204AndPassProjectIdAndTaskIdInCorrectOrder() throws Exception {
    // Why: proves the route path variables are passed as deleteTask(projectId, taskId), not
    // reversed.
    doNothing().when(taskService).deleteTask(10L, 50L);

    mockMvc
        .perform(delete("/api/task/50/project/10"))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));

    verify(taskService).deleteTask(10L, 50L);
  }

  @Test
  void deleteTaskShouldReturnNotFoundWhenTaskIsNotScopedToProjectAndUser() throws Exception {
    // Why: proves delete exposes service-level task scope failure correctly.
    doThrow(new ResourceNotFoundException("Task not found for project 10"))
        .when(taskService)
        .deleteTask(10L, 50L);

    mockMvc
        .perform(delete("/api/task/50/project/10"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Task not found for project 10"));

    verify(taskService).deleteTask(10L, 50L);
  }
}
