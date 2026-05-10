package com.tex.cloud_task_manager.Project;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tex.cloud_task_manager.Project.response_request.CreateProjectRequest;
import com.tex.cloud_task_manager.Project.response_request.ProjectResponse;
import com.tex.cloud_task_manager.Project.response_request.UpdateProjectRequest;
import com.tex.cloud_task_manager.Project.service.ProjectService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/project")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping("/create")
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody CreateProjectRequest request) {
        ProjectResponse response = projectService.createProject(
                request.name(),
                request.description()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
}

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProject(@PathVariable long projectId) {
        return ResponseEntity.ok(projectService.getProject(projectId));
    }

    @PutMapping("/update")
    public ResponseEntity<ProjectResponse> updateProject(@Valid @RequestBody UpdateProjectRequest request) {
        return ResponseEntity.ok(projectService.updateProject(request.projectId(), request.name(), request.description()));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> deleteProject(@PathVariable long projectId) {
        return ResponseEntity.ok(projectService.deleteProject(projectId));
    }

    @GetMapping("/user-projects")
    public ResponseEntity<List<ProjectResponse>> getUserProjects() {
        return ResponseEntity.ok(projectService.getUserProjects());
    }

    // public ResponseEntity<?> getProjectTasks() {
    //     return ResponseEntity.ok("List of tasks for the project");
    // }

    // public ResponseEntity<?> addTaskToProject() {
    //     return ResponseEntity.ok("Task added to project successfully");
    // }

    // public ResponseEntity<?> removeTaskFromProject() {
    //     return ResponseEntity.ok("Task removed from project successfully");
    // }

    // public ResponseEntity<?> getProjectMembers() {
    //     return ResponseEntity.ok("List of project members");
    // }

    // public ResponseEntity<?> addMemberToProject() {
    //     return ResponseEntity.ok("Member added to project successfully");
    // }

    // public ResponseEntity<?> removeMemberFromProject() {
    //     return ResponseEntity.ok("Member removed from project successfully");
    // }

    // public ResponseEntity<?> getProjectActivity() {
    //     return ResponseEntity.ok("Project activity log");
    // }


}
