package com.tex.cloud_task_manager.User;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tex.cloud_task_manager.User.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/allUsers")
    public List<UserEntity> getUsers() {

        return userService.getAllUsers();
    }

}
