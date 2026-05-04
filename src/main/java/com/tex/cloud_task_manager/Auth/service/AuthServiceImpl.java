package com.tex.cloud_task_manager.Auth.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.tex.cloud_task_manager.Auth.response_request.AuthResponse;
import com.tex.cloud_task_manager.Security.CustomUserDetailsService;
import com.tex.cloud_task_manager.Security.JwtService;
import com.tex.cloud_task_manager.User.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {


    private final UserService userService;
    private final CustomUserDetailsService customUserDetailsService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public AuthResponse registerUser(String name, String email, String password) {
       
        if(userService.findByEmail(email).isPresent()) {

            return new AuthResponse("User already exists for this email", null);
        }

          String encodedPassword = passwordEncoder.encode(password);
        userService.createUser(name, email, encodedPassword);
        return new AuthResponse("User registered successfully ", null);
    }

    @Override
    public AuthResponse loginUser(String email, String password) {
  
        try {

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            var userDetails = customUserDetailsService.loadUserByUsername(email);


            String token = jwtService.generateToken(userDetails);

            return new AuthResponse("User logged in successfully", token);

        } 
        catch(BadCredentialsException e) {
            return new AuthResponse("Invalid credentials", null);
        }

    }

}
