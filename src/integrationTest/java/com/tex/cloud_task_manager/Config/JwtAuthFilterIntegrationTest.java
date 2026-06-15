package com.tex.cloud_task_manager.Config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tex.cloud_task_manager.AbstractWebIntegrationTest;
import com.tex.cloud_task_manager.RefreshToken.RefreshTokenRepository;
import com.tex.cloud_task_manager.Security.JwtService;
import com.tex.cloud_task_manager.User.UserEntityRepository;
import com.tex.cloud_task_manager.User.service.UserService;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

class JwtAuthFilterIntegrationTest extends AbstractWebIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtService jwtService;

  @Autowired private UserService userService;

  @Autowired private UserEntityRepository userEntityRepository;

  @Autowired private RefreshTokenRepository refreshTokenRepository;

  @BeforeEach
  void setUp() {
    refreshTokenRepository.deleteAll();
    userEntityRepository.deleteAll();
  }

  @Test
  void protectedEndpointShouldRejectRequestWithoutAccessTokenCookie() throws Exception {
    mockMvc.perform(get("/api/user/allUsers")).andExpect(status().isUnauthorized());
  }

  @Test
  void protectedEndpointShouldAllowRequestWithValidAccessTokenCookie() throws Exception {
    String email = "kevin@test.com";

    userService.createUser("Kevin", email, "encoded-password", "USER");

    assertThat(userEntityRepository.findByEmail(email)).isPresent();

    UserDetails userDetails =
        org.springframework.security.core.userdetails.User.withUsername(email)
            .password("encoded-password")
            .roles("USER")
            .build();

    String token = jwtService.generateToken(userDetails);

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/user/allUsers")
                .cookie(new Cookie("access_token", token)))
        .andExpect(MockMvcResultMatchers.status().isOk());
  }

  @Test
  void refreshEndpointShouldNotRequireValidAccessJwt() throws Exception {
    registerUser();
    String refreshToken = loginAndExtractRefreshToken();

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .header("Authorization", "Bearer not-a-valid-access-token")
                .cookie(new Cookie("refresh_token", refreshToken)))
        .andExpect(status().isNoContent());
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
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isOk());
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
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody))
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
}
