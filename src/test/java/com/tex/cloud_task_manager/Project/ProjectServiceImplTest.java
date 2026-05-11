package com.tex.cloud_task_manager.Project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tex.cloud_task_manager.Project.response_request.ProjectResponse;
import com.tex.cloud_task_manager.Project.service.ProjectServiceImpl;
import com.tex.cloud_task_manager.Security.CurrentUserService;
import com.tex.cloud_task_manager.Task.Priority;
import com.tex.cloud_task_manager.common.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private ProjectEntity project;

    @BeforeEach
    void setUp() {
        project = ProjectEntity.builder()
                .id(1L)
                .userId(10L)
                .name("Project One")
                .description("Project One Description")
                .createdAt(LocalDateTime.now())
                .updatedAt(null)
                .status(ProjectStatus.ACTIVE)
                .priority(Priority.LOW)
                .build();

        project.setTasks(List.of());
    }

    @Test
    void createProjectShouldSaveProjectWithCurrentUserId() {
        when(currentUserService.getCurrentUserId()).thenReturn(10L);

        when(projectRepository.save(any(ProjectEntity.class))).thenAnswer(invocation -> {
            ProjectEntity savedProject = invocation.getArgument(0);
            savedProject.setId(1L);
            savedProject.setTasks(List.of());
            return savedProject;
        });

        ProjectResponse response = projectService.createProject(
                "Project One",
                "Project One Description"
        );

        ArgumentCaptor<ProjectEntity> captor = ArgumentCaptor.forClass(ProjectEntity.class);
        verify(projectRepository).save(captor.capture());

        ProjectEntity savedProject = captor.getValue();

        assertEquals(10L, savedProject.getUserId());
        assertEquals("Project One", savedProject.getName());
        assertEquals("Project One Description", savedProject.getDescription());
        assertEquals(ProjectStatus.ACTIVE, savedProject.getStatus());
        assertEquals(Priority.LOW, savedProject.getPriority());
        assertNotNull(savedProject.getCreatedAt());

        assertEquals(1L, response.id());
        assertEquals("Project One", response.name());
        assertEquals("Project One Description", response.description());
        assertEquals(0, response.taskCount());
        assertEquals(ProjectStatus.ACTIVE, response.status());
    }

    @Test
    void getUserProjectsShouldOnlyAskRepositoryForCurrentUsersProjects() {
        when(currentUserService.getCurrentUserId()).thenReturn(10L);
        when(projectRepository.findByUserId(10L)).thenReturn(Optional.of(List.of(project)));

        List<ProjectResponse> responses = projectService.getUserProjects();

        assertEquals(1, responses.size());

        ProjectResponse response = responses.getFirst();

        assertEquals(1L, response.id());
        assertEquals("Project One", response.name());
        assertEquals("Project One Description", response.description());
        assertEquals(0, response.taskCount());
        assertEquals(ProjectStatus.ACTIVE, response.status());

        verify(projectRepository).findByUserId(10L);
        verify(projectRepository, never()).findAll();
    }

    @Test
    void getUserProjectsShouldThrowWhenNoProjectsFoundForCurrentUser() {
        when(currentUserService.getCurrentUserId()).thenReturn(10L);
        when(projectRepository.findByUserId(10L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> projectService.getUserProjects()
        );

        assertEquals("No Projects found for user 10", exception.getMessage());

        verify(projectRepository).findByUserId(10L);
        verify(projectRepository, never()).findAll();
    }

    @Test
    void getProjectShouldUseProjectIdAndCurrentUserId() {
        when(currentUserService.getCurrentUserId()).thenReturn(10L);
        when(projectRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(project));

        ProjectResponse response = projectService.getProject(1L);

        assertEquals(1L, response.id());
        assertEquals("Project One", response.name());
        assertEquals("Project One Description", response.description());
        assertEquals(0, response.taskCount());

        verify(projectRepository).findByIdAndUserId(1L, 10L);
        verify(projectRepository, never()).findById(any());
    }

    @Test
    void getProjectShouldThrowWhenProjectDoesNotBelongToCurrentUser() {
        when(currentUserService.getCurrentUserId()).thenReturn(20L);
        when(projectRepository.findByIdAndUserId(1L, 20L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> projectService.getProject(1L)
        );

        assertEquals("Project not found with id: 1", exception.getMessage());

        verify(projectRepository).findByIdAndUserId(1L, 20L);
        verify(projectRepository, never()).findById(any());
    }

    @Test
    void updateProjectShouldUseProjectIdAndCurrentUserIdBeforeSaving() {
        when(currentUserService.getCurrentUserId()).thenReturn(10L);
        when(projectRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(ProjectEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResponse response = projectService.updateProject(
                1L,
                "Updated Project",
                "Updated Description"
        );

        assertEquals(1L, response.id());
        assertEquals("Updated Project", response.name());
        assertEquals("Updated Description", response.description());

        ArgumentCaptor<ProjectEntity> captor = ArgumentCaptor.forClass(ProjectEntity.class);
        verify(projectRepository).save(captor.capture());

        ProjectEntity updatedProject = captor.getValue();

        assertEquals(10L, updatedProject.getUserId());
        assertEquals("Updated Project", updatedProject.getName());
        assertEquals("Updated Description", updatedProject.getDescription());
        assertNotNull(updatedProject.getUpdatedAt());

        verify(projectRepository).findByIdAndUserId(1L, 10L);
        verify(projectRepository, never()).findById(any());
    }

    @Test
    void updateProjectShouldNotSaveWhenProjectDoesNotBelongToCurrentUser() {
        when(currentUserService.getCurrentUserId()).thenReturn(20L);
        when(projectRepository.findByIdAndUserId(1L, 20L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> projectService.updateProject(1L, "Bad Update", "Bad Description")
        );

        assertEquals("Project not found with id: 1", exception.getMessage());

        verify(projectRepository).findByIdAndUserId(1L, 20L);
        verify(projectRepository, never()).save(any());
        verify(projectRepository, never()).findById(any());
    }

    @Test
    void deleteProjectShouldUseProjectIdAndCurrentUserIdBeforeDeleting() {
        when(currentUserService.getCurrentUserId()).thenReturn(10L);
        when(projectRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(project));

        ProjectResponse response = projectService.deleteProject(1L);

        assertEquals(1L, response.id());
        assertEquals("Project One", response.name());
        assertEquals(0, response.taskCount());

        verify(projectRepository).findByIdAndUserId(1L, 10L);
        verify(projectRepository).delete(project);
        verify(projectRepository, never()).findById(any());
    }

    @Test
    void deleteProjectShouldNotDeleteWhenProjectDoesNotBelongToCurrentUser() {
        when(currentUserService.getCurrentUserId()).thenReturn(20L);
        when(projectRepository.findByIdAndUserId(1L, 20L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> projectService.deleteProject(1L)
        );

        assertEquals("Project not found with id: 1", exception.getMessage());

        verify(projectRepository).findByIdAndUserId(1L, 20L);
        verify(projectRepository, never()).delete(any());
        verify(projectRepository, never()).findById(any());
    }
}