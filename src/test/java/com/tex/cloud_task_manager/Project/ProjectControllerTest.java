package com.tex.cloud_task_manager.Project;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tex.cloud_task_manager.Project.response_request.CreateProjectRequest;
import com.tex.cloud_task_manager.Project.response_request.ProjectResponse;
import com.tex.cloud_task_manager.Project.response_request.UpdateProjectRequest;
import com.tex.cloud_task_manager.Project.service.ProjectService;
import com.tex.cloud_task_manager.Security.CustomUserDetailsService;
import com.tex.cloud_task_manager.Security.JwtService;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(ProjectController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void createProjectShouldReturnCreatedAndCallProjectService() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest(
                "Cloud Task Manager",
                "Project for organizing tasks"
        );

        ProjectResponse serviceResponse = new ProjectResponse(
                1L,
                "Cloud Task Manager",
                "Project for organizing tasks",
                0,
                LocalDateTime.parse("2026-05-07T10:30:00"),
                LocalDateTime.parse("2026-05-07T10:30:00"),
                ProjectStatus.ACTIVE
        );

        when(projectService.createProject(anyString(), anyString()))
                .thenReturn(serviceResponse);

        mockMvc.perform(post("/api/project/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Cloud Task Manager"))
                .andExpect(jsonPath("$.description").value("Project for organizing tasks"))
                .andExpect(jsonPath("$.taskCount").value(0))
                .andExpect(jsonPath("$.createdAt").value("2026-05-07T10:30:00"))
                .andExpect(jsonPath("$.updatedAt").value("2026-05-07T10:30:00"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(projectService).createProject(anyString(), anyString());
    }

    @Test
    void getUserProjectsShouldReturnOkAndProjectList() throws Exception {
        List<ProjectResponse> serviceResponse = List.of(
                new ProjectResponse(
                        1L,
                        "Cloud Task Manager",
                        "Backend task app",
                        0,
                        LocalDateTime.parse("2026-05-07T10:30:00"),
                        LocalDateTime.parse("2026-05-07T10:30:00"),
                        ProjectStatus.ACTIVE
                ),
                new ProjectResponse(
                        2L,
                        "Portfolio Site",
                        "Personal portfolio project",
                        0,
                        LocalDateTime.parse("2026-05-07T11:00:00"),
                        LocalDateTime.parse("2026-05-07T11:00:00"),
                        ProjectStatus.ACTIVE
                )
        );

        when(projectService.getUserProjects())
                .thenReturn(serviceResponse);

        mockMvc.perform(get("/api/project/user-projects"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Cloud Task Manager"))
                .andExpect(jsonPath("$[0].description").value("Backend task app"))
                .andExpect(jsonPath("$[0].taskCount").value(0))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Portfolio Site"))
                .andExpect(jsonPath("$[1].description").value("Personal portfolio project"))
                .andExpect(jsonPath("$[1].taskCount").value(0))
                .andExpect(jsonPath("$[1].status").value("ACTIVE"));

        verify(projectService).getUserProjects();
    }

    @Test
    void getProjectShouldReturnOkAndProject() throws Exception {
        ProjectResponse serviceResponse = new ProjectResponse(
                1L,
                "Cloud Task Manager",
                "Backend task app",
                0,
                LocalDateTime.parse("2026-05-07T10:30:00"),
                LocalDateTime.parse("2026-05-07T10:30:00"),
                ProjectStatus.ACTIVE
        );

        when(projectService.getProject(1L))
                .thenReturn(serviceResponse);

        mockMvc.perform(get("/api/project/{projectId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Cloud Task Manager"))
                .andExpect(jsonPath("$.description").value("Backend task app"))
                .andExpect(jsonPath("$.taskCount").value(0))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(projectService).getProject(1L);
    }

    @Test
    void updateProjectShouldReturnOkAndCallProjectService() throws Exception {
        UpdateProjectRequest request = new UpdateProjectRequest(
                1L,
                "Updated Project Name",
                "Updated project description"
        );

        ProjectResponse serviceResponse = new ProjectResponse(
                1L,
                "Updated Project Name",
                "Updated project description",
                0,
                LocalDateTime.parse("2026-05-07T10:30:00"),
                LocalDateTime.parse("2026-05-07T12:00:00"),
                ProjectStatus.ACTIVE
        );

        when(projectService.updateProject(1L, "Updated Project Name", "Updated project description"))
                .thenReturn(serviceResponse);

        mockMvc.perform(put("/api/project/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Updated Project Name"))
                .andExpect(jsonPath("$.description").value("Updated project description"))
                .andExpect(jsonPath("$.taskCount").value(0))
                .andExpect(jsonPath("$.createdAt").value("2026-05-07T10:30:00"))
                .andExpect(jsonPath("$.updatedAt").value("2026-05-07T12:00:00"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(projectService).updateProject(anyLong(), anyString(), anyString());
    }


    @Test
    void updateProjectShouldReturnBadRequestWhenNameExceedsMaxLength() throws Exception {
        String longName = "A".repeat(101);

        UpdateProjectRequest request = new UpdateProjectRequest(
                1L,
                longName,
                "Valid description"
        );

        mockMvc.perform(put("/api/project/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProjectShouldReturnBadRequestWhenDescriptionExceedsMaxLength() throws Exception {
        String longDescription = "A".repeat(501);

        UpdateProjectRequest request = new UpdateProjectRequest(
                1L,
                "Valid Name",
                longDescription
        );

        mockMvc.perform(put("/api/project/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteProjectShouldReturnOkAndCallProjectService() throws Exception {
        ProjectResponse serviceResponse = new ProjectResponse(
                1L,
                "Deleted Project",
                "Deleted project description",
                0,
                LocalDateTime.parse("2026-05-07T10:30:00"),
                LocalDateTime.parse("2026-05-07T12:00:00"),
                ProjectStatus.DELETED
        );

        when(projectService.deleteProject(1L))
                .thenReturn(serviceResponse);

        mockMvc.perform(delete("/api/project/{projectId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Deleted Project"))
                .andExpect(jsonPath("$.description").value("Deleted project description"))
                .andExpect(jsonPath("$.status").value("DELETED"));

        verify(projectService).deleteProject(1L);
    }

    @Test
    void createProjectShouldReturnBadRequestWhenRequestBodyIsInvalid() throws Exception {
        String invalidJson = """
                {
                  "name": "",
                  "description": "Valid description"
                }
                """;

        mockMvc.perform(post("/api/project/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}