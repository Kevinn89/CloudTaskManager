package com.tex.cloud_task_manager.Task;

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

import com.tex.cloud_task_manager.Task.response_request.CreateTaskRequest;
import com.tex.cloud_task_manager.Task.response_request.TaskResponse;
import com.tex.cloud_task_manager.Task.response_request.UpdateTaskRequest;
import com.tex.cloud_task_manager.Task.service.TaskService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/task")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping("/create")
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request){

       return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(request.title(), 
       request.description(), request.projectId()));

    }

      @GetMapping("/project/{projectId}")
    public ResponseEntity<List<TaskResponse>> getTasks(@PathVariable long projectId) {
        return ResponseEntity.ok(taskService.getTasks(projectId));
    }


    @PutMapping("/update")
    public ResponseEntity<TaskResponse> updateTask(@Valid @RequestBody UpdateTaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(request.id(), request.projectId(), request.title(), request.description(), request.taskStatus(),
         request.dueDate(), request.completionDate(), request.priority()));
    }

    @DeleteMapping("/{taskId}/project/{projectId}")
    public ResponseEntity<TaskResponse> deleteTask(@PathVariable long projectId, @PathVariable long taskId ) {

             taskService.deleteTask(projectId,taskId);
             
        return ResponseEntity.noContent().build();
    }

}
