package com.tex.cloud_task_manager.User.service;

import com.tex.cloud_task_manager.User.UserEntity;
import com.tex.cloud_task_manager.User.response_request.UserResponse;
import java.util.List;

public interface UserService {

  UserEntity createUser(String name, String email, String password, String accountType);

  List<UserResponse> getAllUsers();

  UserResponse updateUser(String name, String password);

  List<UserResponse> getNonOrgUsers(Long orgId);

}
