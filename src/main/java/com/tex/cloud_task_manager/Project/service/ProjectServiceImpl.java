package com.tex.cloud_task_manager.Project.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final CurrentUserService currentUserService;
    private final ProjectRepository projectRepository;

    @Override
    public ProjectResponse createProject(String name, String description) {

        long userId = currentUserService.getCurrentUserId();

        ProjectEntity project = saveProject(
                ProjectEntity.builder()
                        .userId(userId)
                        .name(name)
                        .description(description)
                        .createdAt(LocalDateTime.now())
                        .status(ProjectStatus.NOT_ACTIVE)
                        .priority(ProjectPriority.LOW)
                        .build());

        List<TaskResponse> tasks = project.getTasks() != null
                ? project.getTasks().stream().map(TaskResponse::from).toList()
                : null;
        int taskCount = project.getTasks() != null ? project.getTasks().size() : 0;

        return ProjectResponse.from(
                project,
                taskCount,
                tasks);
    }

    private ProjectEntity saveProject(ProjectEntity project) {
        return projectRepository.save(project);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> getUserProjects() {

        long userId = currentUserService.getCurrentUserId();

        return projectRepository
                .findByUserId(userId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("No Projects found for user %d".formatted(userId)))
                .stream()
                .map(
                        project -> {
                            List<TaskResponse> tr = project.getTasks() == null
                                    ? List.of()
                                    : project.getTasks().stream().map(TaskResponse::from).toList();

                            return ProjectResponse.from(project, tr.size(), tr);
                        })
                .toList();
    }

    @Override
    @Transactional
    public void deleteProject(long projectId) {

        long userId = currentUserService.getCurrentUserId();

        ProjectEntity projectEntity = projectRepository.findByIdAndUserId(projectId, userId).orElseThrow(
                () -> new ResourceNotFoundException(
                        "Project not found with id: %d".formatted(projectId)));

        projectRepository.delete(projectEntity);
    }

    @Override
    @Transactional
    public ProjectResponse updateProject(long projectId, String name, String description, String priority,
            String status) {

        long userId = currentUserService.getCurrentUserId();

        return projectRepository
                .findByIdAndUserId(projectId, userId)
                .map(
                        project -> {
                            if (name != null && !name.isBlank())
                                project.setName(name);
                            if (description != null && !description.isBlank())
                                project.setDescription(description);
                            if (priority != null && !priority.isBlank()
                                    && !project.getPriority().equals(ProjectPriority.valueOf(priority)))
                                project.setPriority(ProjectPriority.valueOf(priority));
                            if (status != null && !status.isBlank()
                                    && !project.getStatus().equals(ProjectStatus.valueOf(status)))
                                project = updateStatus(project, ProjectStatus.valueOf(status));
                            project.setUpdatedAt(LocalDateTime.now());
                            saveProject(project);

                            return ProjectResponse.from(
                                    project,
                                    project.getTasks().size(),
                                    project.getTasks().stream().map(TaskResponse::from).toList());
                        })
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Project not found with id: %d".formatted(projectId)));
    }

    private ProjectEntity updateStatus(ProjectEntity curreStatus, ProjectStatus toStatus) {

        ProjectStatus status = curreStatus.getStatus();

        switch (status) {
            case ProjectStatus.NOT_ACTIVE:
                if (ProjectStatus.ACTIVE.equals(toStatus))
                    status = toStatus;
                else {
                    throw new BadRequestException(
                            "Unable to move to %s from %s, to be ACTIVE first".formatted(ProjectStatus.NOT_ACTIVE,
                                    toStatus));
                }
                break;
            case ProjectStatus.ACTIVE:
                if (ProjectStatus.COMPLETED.equals(toStatus))
                    curreStatus.setCompletedAt(LocalDateTime.now());
                status = toStatus;
                break;
            case ProjectStatus.COMPLETED:
                if (ProjectStatus.ACTIVE.equals(toStatus))
                    curreStatus.setCompletedAt(null);
                status = toStatus;
            default:
                break;
        }
        curreStatus.setStatus(status);
        return curreStatus;
    }

    @Override
    @Transactional
    public ProjectResponse getProject(long projectId) {

        long userId = currentUserService.getCurrentUserId();

        return projectRepository
                .findByIdAndUserId(projectId, userId)
                .map(
                        project -> {
                            int count = project.getTasks() == null ? 0 : project.getTasks().size();

                            return ProjectResponse.from(
                                    project, count, project.getTasks().stream().map(TaskResponse::from).toList());
                        })
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Project not found with id: %d".formatted(projectId)));
    }

    @Override
    @Transactional
    public ProjectResponse completeProject(long projectId) {

        long userId = currentUserService.getCurrentUserId();

        return projectRepository
                .findByIdAndUserId(projectId, userId)
                .map(
                        project -> {
                            project.getTasks().stream()
                                    .forEach(
                                            task -> {
                                                if (task.getCompletionDate() == null
                                                        || !task.getTaskStatus().equals(TaskStatus.DONE)) {
                                                    throw new BadRequestException(
                                                            "Incomplete Task title %s currently %s"
                                                                    .formatted(task.getTitle(), task.getTaskStatus()));
                                                }
                                            });

                            project.setStatus(ProjectStatus.COMPLETED);
                            project.setPriority(ProjectPriority.LOW);
                            project.setUpdatedAt(LocalDateTime.now());

                            return ProjectResponse.from(project, 0, null);
                        })
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Project not found with id: %d".formatted(projectId)));
    }
}
