package com.tex.cloud_task_manager.Auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;
import com.tex.cloud_task_manager.AbstractWebIntegrationTest;
import com.tex.cloud_task_manager.RefreshToken.RefreshTokenEntity;
import com.tex.cloud_task_manager.RefreshToken.RefreshTokenRepository;
import com.tex.cloud_task_manager.User.UserEntity;
import com.tex.cloud_task_manager.User.UserEntityRepository;

class AuthControllerIntegrationTest extends AbstractWebIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserEntityRepository userEntityRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userEntityRepository.deleteAll();
    }

    @Test
    void registerShouldCreateUserThroughHttpEndpoint() throws Exception {
        String requestBody = """
                {
                  "name": "Kevin",
                  "email": "kevin@test.com",
                  "password": "Password123!"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully "));

        UserEntity savedUser = userEntityRepository.findByEmail("kevin@test.com")
                .orElseThrow();

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getName()).isEqualTo("Kevin");
        assertThat(savedUser.getEmail()).isEqualTo("kevin@test.com");
        assertThat(savedUser.getPassword()).isNotEqualTo("Password123!");
        assertThat(savedUser.getCreatedAt()).isNotNull();
    }

    @Test
    void registerShouldReturnAlreadyExistsMessageWhenEmailAlreadyExists() throws Exception {
       
        registerUser();

        String duplicateBody = """
                {
                  "name": "Kevin Again",
                  "email": "kevin@test.com",
                  "password": "AnotherPassword123!"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateBody))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message").value("Email is already in use"));

        assertThat(userEntityRepository.findAll()).hasSize(1);
    }

    @Test
    void loginShouldReturnJwtAndRefreshTokenThroughHttpEndpoint() throws Exception {
        registerUser();

        String loginBody = """
                {
                  "email": "kevin@test.com",
                  "password": "Password123!"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User logged in successfully"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenExpiration").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshTokenExpiration").isNotEmpty());

        assertThat(refreshTokenRepository.findAll()).hasSize(1);
    }

    @Test
    void loginShouldReturnInvalidCredentialsWhenPasswordIsWrong() throws Exception {
        registerUser();

        String loginBody = """
                {
                  "email": "kevin@test.com",
                  "password": "WrongPassword123!"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));

        assertThat(refreshTokenRepository.findAll()).isEmpty();
    }

    @Test
    void refreshShouldReturnNewJwtWhenRefreshTokenIsValid() throws Exception {
        registerUser();

        String refreshToken = loginAndExtractRefreshToken();

        String refreshBody = """
                {
                  "refreshToken": "%s",
                  "email": "kevin@test.com"
                }
                """.formatted(refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenExpiration").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").value(refreshToken))
                .andExpect(jsonPath("$.refreshTokenExpiration").isNotEmpty());

        RefreshTokenEntity savedToken =
                refreshTokenRepository.findByTokenHashAndRevoked(sha256(refreshToken), false);

        assertThat(savedToken).isNotNull();
        assertThat(savedToken.getLastUsedAt()).isNull();
    }

    @Test
    void logoutShouldRevokeRefreshTokenThroughHttpEndpoint() throws Exception {
       
        registerUser();

        String refreshToken = loginAndExtractRefreshToken();

        String logoutBody = """
                {
                  "refreshToken": "%s"
                }
                """.formatted(refreshToken);

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logoutBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User logged out successfully"));

        RefreshTokenEntity revokedToken =
                refreshTokenRepository.findByTokenHashAndRevoked(sha256(refreshToken), true);

        assertThat(revokedToken).isNotNull();
        assertThat(revokedToken.isRevoked()).isTrue();
        assertThat(revokedToken.getRevokedAt()).isNotNull();
    }

    @Test
    void refreshShouldFailAfterLogout() throws Exception {
        registerUser();

        String refreshToken = loginAndExtractRefreshToken();

        String logoutBody = """
                {
                  "refreshToken": "%s"
                }
                """.formatted(refreshToken);

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logoutBody))
                .andExpect(status().isOk());

        String refreshBody = """
                {
                  "refreshToken": "%s",
                  "email": "kevin@test.com"
                }
                """.formatted(refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isUnauthorized());
    }

    private void registerUser() throws Exception {
        String requestBody = """
                {
                  "name": "Kevin",
                  "email": "kevin@test.com",
                  "password": "Password123!"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    private String loginAndExtractRefreshToken() throws Exception {
        String loginBody = """
                {
                  "email": "kevin@test.com",
                  "password": "Password123!"
                }
                """;

        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        return JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.refreshToken"
        );
    }

    private static String sha256(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}