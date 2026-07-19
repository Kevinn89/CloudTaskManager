package com.tex.cloud_task_manager.Project;

import com.tex.cloud_task_manager.Project.response_request.CreateProjectRequest;
import com.tex.cloud_task_manager.Project.response_request.ProjectResponse;
import com.tex.cloud_task_manager.Project.response_request.UpdateProjectRequest;
import com.tex.cloud_task_manager.Project.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/project")
public class ProjectController {

  private final ProjectService projectService;

  @PreAuthorize("hasAnyRole('ADMIN')")
  @PostMapping("/create")
  public ResponseEntity<ProjectResponse> createProject(
      @Valid @RequestBody CreateProjectRequest request) {
    ProjectResponse response = projectService.createProject(request.name(), request.description());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PreAuthorize("hasAnyRole( 'ADMIN','USER')")
  @GetMapping("/{projectId}")
  public ResponseEntity<ProjectResponse> getProject(@PathVariable("projectId") long projectId) {
    return ResponseEntity.ok(projectService.getProject(projectId));
  }

  @PreAuthorize("hasAnyRole( 'ADMIN','USER')")
  @PutMapping("/update")
  public ResponseEntity<ProjectResponse> updateProject(
      @Valid @RequestBody UpdateProjectRequest request) {
    return ResponseEntity.ok(
        projectService.updateProject(
            request.projectId(),
            request.name(),
            request.description(),
            request.priorityStatus(),
            request.projectStatus()));
  }

  @PreAuthorize("hasAnyRole('ADMIN')")
  @DeleteMapping("/{projectId}")
  public ResponseEntity<Void> deleteProject(@PathVariable("projectId") Long projectId) {
    projectService.deleteProject(projectId);
    return ResponseEntity.noContent().build();
  }

  @PreAuthorize("hasAnyRole('ADMIN','USER')")
  @GetMapping("/user-projects")
  public ResponseEntity<List<ProjectResponse>> getUserProjects() {
    return ResponseEntity.ok(projectService.getUserProjects());
  }

  @PreAuthorize("hasAnyRole('ADMIN','USER')")
  @PutMapping("/{projectId}/complete")
  public ResponseEntity<ProjectResponse> complete(@PathVariable("projectId") long projectId) {
    return ResponseEntity.ok(projectService.completeProject(projectId));
  }
}
