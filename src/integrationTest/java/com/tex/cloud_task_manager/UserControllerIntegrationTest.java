package com.tex.cloud_task_manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tex.cloud_task_manager.Security.JwtService;
import com.tex.cloud_task_manager.User.UserEntity;
import com.tex.cloud_task_manager.User.UserEntityRepository;
import com.tex.cloud_task_manager.User.service.UserService;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

class UserControllerIntegrationTest extends AbstractWebIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtService jwtService;

  @Autowired private UserService userService;

  @Autowired private UserEntityRepository userEntityRepository;

  @BeforeEach
  void setUp() {
    userEntityRepository.deleteAll();
  }

  @Test
  void updateUserShouldPersistChangesForAuthenticatedUser() throws Exception {
    // Arrange
    UserEntity createdUser =
        userService.createUser("Kevin", "kevin@test.com", "old-password", "USER");
    createdUser.setVerifiedAt(Instant.now());
    userEntityRepository.save(createdUser);

    UserDetails userDetails =
        org.springframework.security.core.userdetails.User.withUsername("kevin@test.com")
            .password("old-password")
            .roles("USER")
            .build();

    String token = jwtService.generateToken(userDetails);

    String requestBody =
        """
                {
                  "name": "Kevin Updated",
                  "password": "new-password"
                }
                """;

    // Act + Assert
    mockMvc
        .perform(
            put("/api/user/update")
                .cookie(new Cookie("access_token", token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Kevin Updated"));

    UserEntity updatedUser = userEntityRepository.findById(createdUser.getId()).orElseThrow();

    assertThat(updatedUser.getName()).isEqualTo("Kevin Updated");
    assertThat(updatedUser.getPassword()).isEqualTo("new-password");
    assertThat(updatedUser.getUpdatedAt()).isNotNull();
  }
}
