package com.tex.cloud_task_manager.User;

import com.tex.cloud_task_manager.User.response_request.UpdateUserRequest;
import com.tex.cloud_task_manager.User.response_request.UserResponse;
import com.tex.cloud_task_manager.User.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @PutMapping("/update")
  public ResponseEntity<UserResponse> updateUser(@Valid @RequestBody UpdateUserRequest request) {
    return ResponseEntity.ok(userService.updateUser(request.name(), request.password()));
  }

  @GetMapping("/allUsers")
  public List<UserResponse> getUsers() {

    return userService.getAllUsers();
  }
}
