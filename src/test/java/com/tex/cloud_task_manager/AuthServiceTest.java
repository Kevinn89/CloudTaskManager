package com.tex.cloud_task_manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.tex.cloud_task_manager.Auth.response_request.AuthResponse;
import com.tex.cloud_task_manager.Auth.service.AuthServiceImpl;
import com.tex.cloud_task_manager.Security.CustomUserDetailsService;
import com.tex.cloud_task_manager.Security.JwtService;
import com.tex.cloud_task_manager.User.UserEntity;
import com.tex.cloud_task_manager.User.service.UserService;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private  UserService userService;
    @Mock
    private  CustomUserDetailsService customUserDetailsService;
    @Mock
    private  AuthenticationManager authenticationManager;
    @Mock
    private  PasswordEncoder passwordEncoder;
    @Mock
    private  JwtService jwtService;

    @InjectMocks
    private  AuthServiceImpl authService;

    private UserEntity userEntity;

    private UserDetails userDetail;

    private Authentication auth;

  @BeforeEach
    void setUp() {
        userEntity = UserEntity.builder()
                .id(1L)
                .name("Kevin")
                .email("test@example.com")
                .password("password123")
                .createdAt("2023-01-01")
                .build();
    }


    @Test 
    void test_registerUser_success (){

        when(userService.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userService.createUser("Kevin", "test@example.com", "encoded-password")).thenReturn(userEntity);
        
        AuthResponse authResponse = authService.registerUser("Kevin", "test@example.com", "password123");

        assertEquals(null, authResponse.token());
        assertEquals("User registered successfully ", authResponse.message());

        verify(userService).createUser("Kevin", "test@example.com", "encoded-password");
        verify(userService).findByEmail("test@example.com");
        verify(passwordEncoder).encode("password123");
    }


    @Test 
    void test_loginUser_success (){

        when(authenticationManager.authenticate(new UsernamePasswordAuthenticationToken("test@example.com", "password123"))).thenReturn(auth);
        when(customUserDetailsService.loadUserByUsername("test@example.com")).thenReturn(userDetail);
        when(jwtService.generateToken(userDetail)).thenReturn("test-jwt-token");

       AuthResponse authResponse = authService.loginUser( "test@example.com", "password123");

       assertEquals("test-jwt-token", authResponse.token());
       assertEquals("User logged in successfully", authResponse.message());

        verify(authenticationManager).authenticate(new UsernamePasswordAuthenticationToken("test@example.com", "password123"));
        verify(customUserDetailsService).loadUserByUsername("test@example.com");
        verify(jwtService).generateToken(userDetail);
    }

    @Test 
    void test_registerUser_userExists_failure (){

        
        when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(userEntity));
        AuthResponse authResponse = authService.registerUser("Kevin", "test@example.com", "password123");

        assertEquals(null, authResponse.token());
        assertEquals("User already exists for this email", authResponse.message());

        verify(userService).findByEmail("test@example.com");
        verify(userService, never()).createUser(anyString(), anyString(), anyString());
        verify(passwordEncoder, never()).encode(anyString());

        verifyNoInteractions(jwtService);

    }

    @Test 
    void test_loginUser_BadCredentialsException_failure (){

        when(authenticationManager.authenticate(new UsernamePasswordAuthenticationToken("test@example.com", "password123"))).thenThrow(new BadCredentialsException("Invalid credentials"));

       AuthResponse authResponse = authService.loginUser( "test@example.com", "password123");

       assertEquals(null, authResponse.token());
       assertEquals("Invalid credentials", authResponse.message());

        verify(authenticationManager).authenticate(new UsernamePasswordAuthenticationToken("test@example.com", "password123"));
        verifyNoInteractions(customUserDetailsService);
        verifyNoInteractions(jwtService);
    }

}
