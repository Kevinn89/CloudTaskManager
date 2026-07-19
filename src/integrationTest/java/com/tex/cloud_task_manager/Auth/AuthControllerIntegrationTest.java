package com.tex.cloud_task_manager.Auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tex.cloud_task_manager.AbstractWebIntegrationTest;
import com.tex.cloud_task_manager.RefreshToken.RefreshTokenEntity;
import com.tex.cloud_task_manager.RefreshToken.RefreshTokenRepository;
import com.tex.cloud_task_manager.User.UserEntity;
import com.tex.cloud_task_manager.User.UserEntityRepository;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class AuthControllerIntegrationTest extends AbstractWebIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private UserEntityRepository userEntityRepository;

  @Autowired private RefreshTokenRepository refreshTokenRepository;

  @BeforeEach
  void setUp() {
    refreshTokenRepository.deleteAll();
    userEntityRepository.deleteAll();
  }

  @Test
  void registerShouldCreateUserThroughHttpEndpoint() throws Exception {
    String requestBody =
        """
                {
                  "name": "Kevin",
                  "email": "kevin@test.com",
                  "password": "Password123!",
                  "accountType": "USER"
                }
                """;

    mockMvc
        .perform(
            post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("User registered successfully "));

    UserEntity savedUser = userEntityRepository.findByEmail("kevin@test.com").orElseThrow();

    assertThat(savedUser.getId()).isNotNull();
    assertThat(savedUser.getName()).isEqualTo("Kevin");
    assertThat(savedUser.getEmail()).isEqualTo("kevin@test.com");
    assertThat(savedUser.getAccountType()).isEqualTo("USER");
    assertThat(savedUser.getPassword()).isNotEqualTo("Password123!");
    assertThat(savedUser.getCreatedAt()).isNotNull();
  }

  @Test
  void registerShouldReturnAlreadyExistsMessageWhenEmailAlreadyExists() throws Exception {

    registerUser();

    String duplicateBody =
        """
                {
                  "name": "Kevin Again",
                  "email": "kevin@test.com",
                  "password": "AnotherPassword123!",
                  "accountType": "USER"
                }
                """;

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(duplicateBody))
        .andExpect(status().is4xxClientError())
        .andExpect(jsonPath("$.message").value("Email is already in use"));

    assertThat(userEntityRepository.findAll()).hasSize(1);
  }

  @Test
  void loginShouldSetJwtAndRefreshTokenCookiesThroughHttpEndpoint() throws Exception {
    registerUser();

    String loginBody =
        """
                {
                  "email": "kevin@test.com",
                  "password": "Password123!"
                }
                """;

    mockMvc
        .perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Successfully Login"))
        .andExpect(jsonPath("$.token").isEmpty())
        .andExpect(jsonPath("$.refreshToken").isEmpty())
        .andExpect(jsonPath("$.privileges[0]").value("UPDATE"))
        .andExpect(jsonPath("$.privileges[1]").value("READ"));

    assertThat(refreshTokenRepository.findAll()).hasSize(1);
  }

  @Test
  void loginShouldReturnInvalidCredentialsWhenPasswordIsWrong() throws Exception {
    registerUser();

    String loginBody =
        """
                {
                  "email": "kevin@test.com",
                  "password": "WrongPassword123!"
                }
                """;

    mockMvc
        .perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
        .andExpect(status().is4xxClientError())
        .andExpect(jsonPath("$.message").value("Invalid credentials"));

    assertThat(refreshTokenRepository.findAll()).isEmpty();
  }

  @Test
  void refreshShouldReturnNewJwtWhenRefreshTokenIsValid() throws Exception {
    registerUser();

    String refreshToken = loginAndExtractRefreshToken();

    mockMvc
        .perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", refreshToken)))
        .andExpect(status().isNoContent())
        .andExpect(
            result ->
                assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
                    .anySatisfy(header -> assertThat(header).contains("access_token=")));

    RefreshTokenEntity savedToken =
        refreshTokenRepository.findByTokenHashAndRevoked(sha256(refreshToken), false);

    assertThat(savedToken).isNotNull();
    assertThat(savedToken.getLastUsedAt()).isNull();
  }

  @Test
  void logoutShouldRevokeRefreshTokenThroughHttpEndpoint() throws Exception {

    registerUser();

    String refreshToken = loginAndExtractRefreshToken();

    mockMvc
        .perform(post("/api/auth/logout").cookie(new Cookie("refresh_token", refreshToken)))
        .andExpect(status().isNoContent())
        .andExpect(
            result ->
                assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
                    .anySatisfy(
                        header ->
                            assertThat(header).contains("access_token=").contains("Max-Age=0"))
                    .anySatisfy(
                        header ->
                            assertThat(header).contains("refresh_token=").contains("Max-Age=0")));

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

    mockMvc
        .perform(post("/api/auth/logout").cookie(new Cookie("refresh_token", refreshToken)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", refreshToken)))
        .andExpect(status().isUnauthorized());
  }

  private void registerUser() throws Exception {
    String requestBody =
        """
                {
                  "name": "Kevin",
                  "email": "kevin@test.com",
                  "password": "Password123!",
                  "accountType": "USER"
                }
                """;

    mockMvc
        .perform(
            post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(requestBody))
        .andExpect(status().isOk());

    UserEntity user = userEntityRepository.findByEmail("kevin@test.com").orElseThrow();
    user.setVerifiedAt(Instant.now());
    userEntityRepository.save(user);
  }

  private String loginAndExtractRefreshToken() throws Exception {
    String loginBody =
        """
                {
                  "email": "kevin@test.com",
                  "password": "Password123!"
                }
                """;

    var result =
        mockMvc
            .perform(
                post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
            .andExpect(status().isOk())
            .andReturn();

    return extractCookieValue(
        result.getResponse().getHeaders(HttpHeaders.SET_COOKIE), "refresh_token");
  }

  private static String extractCookieValue(List<String> setCookieHeaders, String cookieName) {
    return setCookieHeaders.stream()
        .filter(header -> header.startsWith(cookieName + "="))
        .map(header -> header.substring((cookieName + "=").length(), header.indexOf(';')))
        .findFirst()
        .orElseThrow();
  }

  private static String sha256(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");

      byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));

      return HexFormat.of().formatHex(hashBytes);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
