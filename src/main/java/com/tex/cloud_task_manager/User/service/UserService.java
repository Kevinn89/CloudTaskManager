package com.tex.cloud_task_manager.User.service;

import java.util.List;
import java.util.Optional;

import com.tex.cloud_task_manager.User.UserEntity;

public interface UserService {

    UserEntity createUser(String name, String email, String password);
    Optional<UserEntity> getUserById(Long id);
    Optional<UserEntity> findByEmail(String email);
    void deleteUser(Long id);
    List<UserEntity> getAllUsers();
}
