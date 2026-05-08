package com.tex.cloud_task_manager.Auth;


import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tex.cloud_task_manager.Auth.response_request.AuthResponse;
import com.tex.cloud_task_manager.Auth.response_request.LogOutRequest;
import com.tex.cloud_task_manager.Auth.response_request.LoginRequest;
import com.tex.cloud_task_manager.Auth.service.AuthService;
import com.tex.cloud_task_manager.RefreshToken.response_request.RefreshTokenRequest;
import com.tex.cloud_task_manager.RefreshToken.service.RefreshTokenService;
import com.tex.cloud_task_manager.Security.CustomUserDetailsService;
import com.tex.cloud_task_manager.Security.JwtService;
import com.tex.cloud_task_manager.User.response_request.RegisterRequest;
import com.tex.cloud_task_manager.User.service.UserService;

import tools.jackson.databind.ObjectMapper; //need to import this for objectMapper to work in spring boot 4

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private RefreshTokenService refreshTokenService;  

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

@Test
void registerShouldReturnOkAndCallAuthService() throws Exception {
    // Arrange
    RegisterRequest request = new RegisterRequest(
            "kevin@test.com",
            "Password123!",
            "Kevin",
            null
    );

    AuthResponse serviceResponse = new AuthResponse(
            "User registered successfully ",
            null,
            null,
            null,
            null
    );

    when(authService.registerUser(anyString(), anyString(), anyString()))
            .thenReturn(serviceResponse);

    // Act + Assert
    mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("User registered successfully "))
            .andExpect(jsonPath("$.token").isEmpty())
            .andExpect(jsonPath("$.refreshToken").isEmpty());

    verify(authService).registerUser(
            "Kevin",
            "kevin@test.com",
            "Password123!"
    );
}

    @Test
    void loginShouldReturnOkAndTokensWhenCredentialsAreValid() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest(
                "kevin@test.com",
                "Password123!"
        );

        AuthResponse serviceResponse = new AuthResponse(
                "User logged in successfully",
                "jwt-token",
                "jwt-expiration",
                "refresh-token",
                "refresh-expiration"
        );

        when(authService.loginUser("kevin@test.com", "Password123!"))
                .thenReturn(serviceResponse);

        // Act + Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User logged in successfully"))
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.tokenExpiration").value("jwt-expiration"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.refreshTokenExpiration").value("refresh-expiration"));

        verify(authService).loginUser("kevin@test.com", "Password123!");
    }

    @Test
    void loginShouldReturnOkWithInvalidCredentialsMessageWhenServiceRejectsLogin() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest(
                "kevin@test.com",
                "wrong-password"
        );

        AuthResponse serviceResponse = new AuthResponse(
                "Invalid credentials",
                null,
                null,
                null,
                null
        );

        when(authService.loginUser("kevin@test.com", "wrong-password"))
                .thenReturn(serviceResponse);

        // Act + Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Invalid credentials"))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist());

        verify(authService).loginUser("kevin@test.com", "wrong-password");
    }

    @Test
    void logoutShouldReturnOkAndCallAuthService() throws Exception {
        // Arrange
        LogOutRequest request = new LogOutRequest("refresh-token");

        AuthResponse serviceResponse = new AuthResponse(
                "User logged out successfully",
                null,
                null,
                null,
                null
        );

        when(authService.logout("refresh-token"))
                .thenReturn(serviceResponse);

        // Act + Assert
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User logged out successfully"))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist());

        verify(authService).logout("refresh-token");
    }

    @Test
    void refreshShouldReturnOkWithNewTokenAndCallAuthService() throws Exception {
    // Arrange
    RefreshTokenRequest request = new RefreshTokenRequest(
            "refresh-token",
            "kevin@test.com"
    );

    AuthResponse serviceResponse = new AuthResponse(
            "Token refreshed successfully",
            "new-jwt-token",
            "jwt-expiration",
            "refresh-token",
            "refresh-expiration"
    );

    when(authService.refresh("refresh-token", "kevin@test.com"))
            .thenReturn(serviceResponse);

    // Act + Assert
    mockMvc.perform(post("/api/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
            .andExpect(jsonPath("$.token").value("new-jwt-token"))
            .andExpect(jsonPath("$.tokenExpiration").value("jwt-expiration"))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
            .andExpect(jsonPath("$.refreshTokenExpiration").value("refresh-expiration"));

    verify(authService).refresh("refresh-token", "kevin@test.com");
   }

    @Test
    void registerShouldReturnBadRequestWhenRequestBodyIsInvalid() throws Exception {
        // Arrange
        String invalidJson = """
                {
                  "name": "",
                  "email": "not-an-email",
                  "password": ""
                }
                """;

        // Act + Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}
