package com.tex.cloud_task_manager.Task.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.tex.cloud_task_manager.Project.ProjectRepository;
import com.tex.cloud_task_manager.Security.CurrentUserService;
import com.tex.cloud_task_manager.Task.Priority;
import com.tex.cloud_task_manager.Task.TaskEntity;
import com.tex.cloud_task_manager.Task.TaskRepository;
import com.tex.cloud_task_manager.Task.TaskStatus;
import com.tex.cloud_task_manager.Task.response_request.TaskResponse;
import com.tex.cloud_task_manager.common.exception.ResourceNotFoundException;
import com.tex.cloud_task_manager.common.exception.UnauthorizedException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final CurrentUserService currentUserService;

    private long getCurrentUserId(){

      return  currentUserService.getCurrentUserId();

    }

    @Override
    public TaskResponse create(String title, String description, long projectId) {

        long userId = getCurrentUserId();

        var project = projectRepository.findByIdAndUserId(projectId,userId).orElseThrow(() -> new ResourceNotFoundException("Project not found for projectId " + projectId));

        TaskEntity taskEntity = TaskEntity.builder()
        .title(title)
        .project(project)
        .userId(userId)
        .description(description)
        .priority(Priority.LOW)
        .taskStatus(TaskStatus.TODO)
        .createdAt(LocalDateTime.now())
        .build();

        return TaskResponse.from(taskRepository.save(taskEntity));
    }

    @Override
    public List<TaskResponse> getTasks(long projectId) {

        var project = projectRepository.findByIdAndUserId(projectId,getCurrentUserId()).orElseThrow(() -> new ResourceNotFoundException("Project not found for projectId " + projectId));

        List<TaskEntity> taskEntity = taskRepository.findByProjectIdAndUserId(projectId,project.getUserId());

        return taskEntity.stream().map(TaskResponse::from).toList();
    }

    @Override
    public TaskResponse updateTask(long taskId,long projectId, String title, String description, String taskStatus, String dueDate, String completionDate, String priority) {
           
        var project = projectRepository.findByIdAndUserId(projectId,getCurrentUserId()).orElseThrow(() -> new ResourceNotFoundException("Project not found for projectId " + projectId));

        TaskEntity taskEntity = taskRepository.findByIdAndProjectIdAndUserId(taskId, project.getId(),project.getUserId()).orElseThrow(() -> new ResourceNotFoundException("Task not found for project " + projectId));

        taskEntity.setDescription(description);
        taskEntity.setDueDate(LocalDate.parse(dueDate));
        taskEntity.setTitle(title);
        taskEntity.setTaskStatus(TaskStatus.valueOf(taskStatus));
        taskEntity.setCompletionDate(LocalDate.parse(completionDate));
        taskEntity.setPriority(Priority.valueOf(priority));
        taskEntity.setProject(project);

      return TaskResponse.from(taskRepository.save(taskEntity));
     
    }

    @Override
    public void deleteTask(long projectId,long taskId) {

        var project = projectRepository.findByIdAndUserId(projectId,getCurrentUserId()).orElseThrow(() -> new UnauthorizedException("Invalid Project"));

        TaskEntity taskEntity = taskRepository.findByIdAndProjectIdAndUserId(taskId,project.getId(),project.getUserId()).orElseThrow(() -> new ResourceNotFoundException("Task not found for project " + projectId));
    
        taskRepository.delete(taskEntity);
    }


}
