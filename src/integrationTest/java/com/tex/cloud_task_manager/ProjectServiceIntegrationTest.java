package com.tex.cloud_task_manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.tex.cloud_task_manager.Project.ProjectEntity;
import com.tex.cloud_task_manager.Project.ProjectRepository;
import com.tex.cloud_task_manager.Project.ProjectStatus;
import com.tex.cloud_task_manager.Project.response_request.ProjectResponse;
import com.tex.cloud_task_manager.Project.service.ProjectService;
import com.tex.cloud_task_manager.Security.CurrentUserService;

class ProjectServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectRepository projectRepository;

    @MockitoBean
    private CurrentUserService currentUserService;

    @BeforeEach
    void setUp() {
        projectRepository.deleteAll();
    }

    @Test
    void createProjectShouldPersistProjectWithCurrentUserId() {
        when(currentUserService.getCurrentUserId()).thenReturn(10L);

        ProjectResponse response = projectService.createProject(
                "Integration Project",
                "Integration Description"
        );

        assertNotNull(response.id());
        assertEquals("Integration Project", response.name());
        assertEquals("Integration Description", response.description());
        assertEquals(ProjectStatus.ACTIVE, response.status());

        ProjectEntity savedProject = projectRepository.findById(response.id())
                .orElseThrow();

        assertEquals(10L, savedProject.getUserId());
        assertEquals("Integration Project", savedProject.getName());
        assertEquals("Integration Description", savedProject.getDescription());
        assertEquals(ProjectStatus.ACTIVE, savedProject.getStatus());
        assertNotNull(savedProject.getCreatedAt());
    }

    @Test
    void getUserProjectsShouldReturnOnlyCurrentUsersProjects() {
        projectRepository.save(ProjectEntity.builder()
                .userId(10L)
                .name("User Ten Project One")
                .description("Owned by user ten")
                .createdAt(LocalDateTime.now())
                .status(ProjectStatus.ACTIVE)
                .build());

        projectRepository.save(ProjectEntity.builder()
                .userId(10L)
                .name("User Ten Project Two")
                .description("Also owned by user ten")
                .createdAt(LocalDateTime.now())
                .status(ProjectStatus.ACTIVE)
                .build());

        projectRepository.save(ProjectEntity.builder()
                .userId(20L)
                .name("User Twenty Project")
                .description("Should not be returned")
                .createdAt(LocalDateTime.now())
                .status(ProjectStatus.ACTIVE)
                .build());

        when(currentUserService.getCurrentUserId()).thenReturn(10L);

        List<ProjectResponse> responses = projectService.getUserProjects();

        assertEquals(2, responses.size());

        List<String> names = responses.stream()
                .map(ProjectResponse::name)
                .toList();

        assertTrue(names.contains("User Ten Project One"));
        assertTrue(names.contains("User Ten Project Two"));
        assertFalse(names.contains("User Twenty Project"));
    }

    @Test
    void getProjectShouldReturnProjectWhenOwnedByCurrentUser() {
        ProjectEntity savedProject = projectRepository.save(ProjectEntity.builder()
                .userId(10L)
                .name("Owned Project")
                .description("Current user owns this")
                .createdAt(LocalDateTime.now())
                .status(ProjectStatus.ACTIVE)
                .build());

        when(currentUserService.getCurrentUserId()).thenReturn(10L);

        ProjectResponse response = projectService.getProject(savedProject.getId());

        assertEquals(savedProject.getId(), response.id());
        assertEquals("Owned Project", response.name());
        assertEquals("Current user owns this", response.description());
    }

    @Test
    void getProjectShouldThrowWhenProjectBelongsToAnotherUser() {
        ProjectEntity otherUsersProject = projectRepository.save(ProjectEntity.builder()
                .userId(20L)
                .name("Other User Project")
                .description("Current user should not access this")
                .createdAt(LocalDateTime.now())
                .status(ProjectStatus.ACTIVE)
                .build());

        when(currentUserService.getCurrentUserId()).thenReturn(10L);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> projectService.getProject(otherUsersProject.getId())
        );

        assertEquals(
                "Project not found with id: " + otherUsersProject.getId(),
                exception.getMessage()
        );
    }

    @Test
    void updateProjectShouldUpdateOnlyWhenOwnedByCurrentUser() {
        ProjectEntity savedProject = projectRepository.save(ProjectEntity.builder()
                .userId(10L)
                .name("Old Name")
                .description("Old Description")
                .createdAt(LocalDateTime.now())
                .status(ProjectStatus.ACTIVE)
                .build());

        when(currentUserService.getCurrentUserId()).thenReturn(10L);

        ProjectResponse response = projectService.updateProject(
                savedProject.getId(),
                "New Name",
                "New Description"
        );

        assertEquals("New Name", response.name());
        assertEquals("New Description", response.description());

        ProjectEntity updatedProject = projectRepository.findById(savedProject.getId())
                .orElseThrow();

        assertEquals(10L, updatedProject.getUserId());
        assertEquals("New Name", updatedProject.getName());
        assertEquals("New Description", updatedProject.getDescription());
    }

    @Test
    void updateProjectShouldThrowAndNotUpdateWhenProjectBelongsToAnotherUser() {
        ProjectEntity otherUsersProject = projectRepository.save(ProjectEntity.builder()
                .userId(20L)
                .name("Original Name")
                .description("Original Description")
                .createdAt(LocalDateTime.now())
                .status(ProjectStatus.ACTIVE)
                .build());

        when(currentUserService.getCurrentUserId()).thenReturn(10L);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> projectService.updateProject(
                        otherUsersProject.getId(),
                        "Hacked Name",
                        "Hacked Description"
                )
        );

        assertEquals(
                "Project not found with id: " + otherUsersProject.getId(),
                exception.getMessage()
        );

        ProjectEntity unchangedProject = projectRepository.findById(otherUsersProject.getId())
                .orElseThrow();

        assertEquals("Original Name", unchangedProject.getName());
        assertEquals("Original Description", unchangedProject.getDescription());
    }

    @Test
    void deleteProjectShouldDeleteOnlyWhenOwnedByCurrentUser() {
        ProjectEntity savedProject = projectRepository.save(ProjectEntity.builder()
                .userId(10L)
                .name("Delete Me")
                .description("Current user owns this")
                .createdAt(LocalDateTime.now())
                .status(ProjectStatus.ACTIVE)
                .build());

        when(currentUserService.getCurrentUserId()).thenReturn(10L);

        ProjectResponse response = projectService.deleteProject(savedProject.getId());

        assertEquals(savedProject.getId(), response.id());
        assertFalse(projectRepository.findById(savedProject.getId()).isPresent());
    }

    @Test
    void deleteProjectShouldThrowAndNotDeleteWhenProjectBelongsToAnotherUser() {
        ProjectEntity otherUsersProject = projectRepository.save(ProjectEntity.builder()
                .userId(20L)
                .name("Do Not Delete")
                .description("Current user does not own this")
                .createdAt(LocalDateTime.now())
                .status(ProjectStatus.ACTIVE)
                .build());

        when(currentUserService.getCurrentUserId()).thenReturn(10L);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> projectService.deleteProject(otherUsersProject.getId())
        );

        assertEquals(
                "Project not found with id: " + otherUsersProject.getId(),
                exception.getMessage()
        );

        assertTrue(projectRepository.findById(otherUsersProject.getId()).isPresent());
    }
}
