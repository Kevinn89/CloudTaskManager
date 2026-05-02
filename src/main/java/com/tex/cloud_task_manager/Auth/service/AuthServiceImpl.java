package com.tex.cloud_task_manager.Auth.service;

import org.springframework.stereotype.Service;

import com.tex.cloud_task_manager.Auth.response_request.AuthResponse;
import com.tex.cloud_task_manager.User.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {


    private final UserService userService;

    @Override
    public AuthResponse registerUser(String name, String email, String password) {
       
        if(userService.findByEmail(email).isPresent()) {

            return new AuthResponse("User already exists for this email");
        }
        userService.createUser(name, email, password);
        return new AuthResponse("User registered successfully ");
    }

    @Override
    public AuthResponse loginUser(String email, String password) {
  
          if(userService.findByEmail(email).get().getPassword().equals(password)) {
              return new AuthResponse("User logged in successfully");
          }

       return new AuthResponse("Invalid email or password");
    }

}
