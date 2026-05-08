package com.tex.cloud_task_manager.User.service;

import java.util.List;

import com.tex.cloud_task_manager.User.UserEntity;

public interface UserService {

    UserEntity createUser(String name, String email, String password);
    List<UserEntity> getAllUsers();
}
