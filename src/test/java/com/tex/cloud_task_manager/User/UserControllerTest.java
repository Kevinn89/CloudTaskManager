package com.tex.cloud_task_manager.User;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tex.cloud_task_manager.Security.CustomUserDetailsService;
import com.tex.cloud_task_manager.Security.JwtService;
import com.tex.cloud_task_manager.User.response_request.UserResponse;
import com.tex.cloud_task_manager.User.service.UserService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private UserService userService;

  /*
   * Only needed if your JwtAuthFilter gets created during @WebMvcTest.
   * If the test passes without these, remove them.
   */
  @MockitoBean private JwtService jwtService;

  @MockitoBean private CustomUserDetailsService customUserDetailsService;

  @Test
  void updateUserShouldReturnUpdatedUserResponse() throws Exception {
    // Arrange
    when(userService.updateUser("Kevin Updated", "NewPassword123!"))
        .thenReturn(new UserResponse(1L, "Kevin Updated"));

    String requestBody =
        """
                                {
                                  "name": "Kevin Updated",
                                  "password": "NewPassword123!"
                                }
                                """;

    // Act + Assert
    mockMvc
        .perform(
            put("/api/user/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.name").value("Kevin Updated"));

    verify(userService).updateUser("Kevin Updated", "NewPassword123!");
  }

  @Test
  void getUsersShouldReturnAllUsers() throws Exception {
    // Arrange
    UserEntity userOne =
        UserEntity.builder()
            .id(1L)
            .name("Kevin")
            .email("kevin@test.com")
            .password("encoded-password-one")
            .createdAt(LocalDateTime.of(2026, 5, 5, 12, 0))
            .updatedAt(null)
            .build();

    UserEntity userTwo =
        UserEntity.builder()
            .id(2L)
            .name("Alex")
            .email("alex@test.com")
            .password("encoded-password-two")
            .createdAt(LocalDateTime.of(2026, 5, 5, 13, 0))
            .updatedAt(null)
            .build();

    when(userService.getAllUsers())
        .thenReturn(List.of(UserResponse.from(userOne), UserResponse.from(userTwo)));

    // Act + Assert
    mockMvc
        .perform(get("/api/user/allUsers").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].name").value("Kevin"))
        .andExpect(jsonPath("$[1].name").value("Alex"));

    verify(userService).getAllUsers();
  }

  @Test
  void getUsersShouldReturnEmptyListWhenNoUsersExist() throws Exception {
    // Arrange
    when(userService.getAllUsers()).thenReturn(List.of());

    // Act + Assert
    mockMvc
        .perform(get("/api/user/allUsers").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(0));

    verify(userService).getAllUsers();
  }
}
