package com.tex.cloud_task_manager.Auth.service;

import com.tex.cloud_task_manager.Auth.response_request.AuthApiReponse;

public interface AuthService {

   AuthApiReponse registerUser(String name, String email, String password);
   AuthApiReponse loginUser(String email, String password);
   AuthApiReponse logout(String token);
   AuthApiReponse refresh(String refreshToken, String email);


}
