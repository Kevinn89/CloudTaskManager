package com.tex.cloud_task_manager.Auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tex.cloud_task_manager.Auth.response_request.AuthResponse;
import com.tex.cloud_task_manager.Auth.response_request.LoginRequest;
import com.tex.cloud_task_manager.Auth.service.AuthService;
import com.tex.cloud_task_manager.Config.JwtProperties;
import com.tex.cloud_task_manager.RefreshToken.service.RefreshTokenService;
import com.tex.cloud_task_manager.Security.CustomUserDetailsService;
import com.tex.cloud_task_manager.Security.JwtService;
import com.tex.cloud_task_manager.User.response_request.RegisterRequest;
import com.tex.cloud_task_manager.User.service.UserService;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private AuthService authService;

  @MockitoBean private JwtService jwtService;

  @MockitoBean private RefreshTokenService refreshTokenService;

  @MockitoBean private UserService userService;

  @MockitoBean private CustomUserDetailsService customUserDetailsService;

  @MockitoBean private JwtProperties jwtProps;

  @BeforeEach
  void setUp() {
    when(jwtProps.accessTokenExpirationMinutes()).thenReturn(15L);
    when(jwtProps.refreshTokenExpirationDays()).thenReturn(7L);
  }

  @Test
  void registerShouldReturnOkAndCallAuthService() throws Exception {
    RegisterRequest request =
        new RegisterRequest("kevin@test.com", "Password123!", "Kevin", "USER");

    AuthResponse serviceResponse =
        new AuthResponse("User registered successfully ", null, null, null);

    when(authService.registerUser("Kevin", "kevin@test.com", "Password123!", "USER"))
        .thenReturn(serviceResponse);

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("User registered successfully "))
        .andExpect(jsonPath("$.token").isEmpty())
        .andExpect(jsonPath("$.refreshToken").isEmpty());

    verify(authService).registerUser("Kevin", "kevin@test.com", "Password123!", "USER");
  }

  @Test
  void loginShouldSetTokenCookiesAndReturnPrivilegesWhenCredentialsAreValid() throws Exception {
    LoginRequest request = new LoginRequest("kevin@test.com", "Password123!");

    AuthResponse serviceResponse =
        new AuthResponse(
            "User logged in successfully",
            "jwt-token",
            "refresh-token",
            List.of(Privilege.UPDATE, Privilege.READ));

    when(authService.loginUser("kevin@test.com", "Password123!")).thenReturn(serviceResponse);

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(
            result ->
                assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
                    .anySatisfy(header -> assertThat(header).contains("access_token=jwt-token"))
                    .anySatisfy(
                        header -> assertThat(header).contains("refresh_token=refresh-token")))
        .andExpect(jsonPath("$.message").value("Successfully Login"))
        .andExpect(jsonPath("$.token").isEmpty())
        .andExpect(jsonPath("$.refreshToken").isEmpty())
        .andExpect(jsonPath("$.errorCode").doesNotExist())
        .andExpect(jsonPath("$.privileges[0]").value("UPDATE"))
        .andExpect(jsonPath("$.privileges[1]").value("READ"));

    verify(authService).loginUser("kevin@test.com", "Password123!");
  }

  @Test
  void refreshShouldSetNewAccessTokenCookieWhenRefreshCookieExists() throws Exception {
    AuthResponse serviceResponse =
        new AuthResponse("Token refreshed successfully", "new-jwt-token", "refresh-token", null);

    when(authService.refresh("refresh-token")).thenReturn(serviceResponse);

    mockMvc
        .perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", "refresh-token")))
        .andExpect(status().isNoContent())
        .andExpect(
            header().string(HttpHeaders.SET_COOKIE, containsString("access_token=new-jwt-token")));

    verify(authService).refresh("refresh-token");
  }

  @Test
  void refreshShouldReturnUnauthorizedWhenRefreshCookieIsMissing() throws Exception {
    mockMvc.perform(post("/api/auth/refresh")).andExpect(status().isUnauthorized());
  }

  @Test
  void logoutShouldClearTokenCookiesAndCallAuthServiceWithRefreshCookie() throws Exception {
    AuthResponse serviceResponse =
        new AuthResponse("User logged out successfully", null, null, null);

    when(authService.logout("refresh-token")).thenReturn(serviceResponse);

    mockMvc
        .perform(post("/api/auth/logout").cookie(new Cookie("refresh_token", "refresh-token")))
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

    verify(authService).logout("refresh-token");
  }

  @Test
  void logoutShouldReturnUnauthorizedWhenRefreshCookieIsMissing() throws Exception {
    mockMvc.perform(post("/api/auth/logout")).andExpect(status().isUnauthorized());
  }

  @Test
  void registerShouldReturnBadRequestWhenRequestBodyIsInvalid() throws Exception {
    String invalidJson =
        """
                {
                  "name": "",
                  "email": "not-an-email",
                  "password": "",
                  "accountType": ""
                }
                """;

    mockMvc
        .perform(
            post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(invalidJson))
        .andExpect(status().isBadRequest());
  }
}
