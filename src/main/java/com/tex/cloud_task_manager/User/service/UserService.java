package com.tex.cloud_task_manager.User.service;

import java.util.List;

import com.tex.cloud_task_manager.User.UserEntity;
import com.tex.cloud_task_manager.User.response_request.UserResponse;

public interface UserService {

    UserEntity createUser(String name, String email, String password);
    List<UserEntity> getAllUsers();
    UserResponse updateUser(String name, String password);
}
