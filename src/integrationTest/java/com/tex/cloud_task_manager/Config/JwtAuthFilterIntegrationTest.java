package com.tex.cloud_task_manager.Config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.tex.cloud_task_manager.AbstractWebIntegrationTest;
import com.tex.cloud_task_manager.Security.JwtService;
import com.tex.cloud_task_manager.User.UserEntityRepository;
import com.tex.cloud_task_manager.User.service.UserService;

class JwtAuthFilterIntegrationTest extends AbstractWebIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserEntityRepository userEntityRepository;

    @Test
    void protectedEndpointShouldRejectRequestWithoutBearerToken() throws Exception {
        mockMvc.perform(get("/allUsers"))
                .andExpect(status().isUnauthorized());
    }

   @Test
    void protectedEndpointShouldAllowRequestWithValidBearerToken() throws Exception {
    String email = "kevin@test.com";

    userEntityRepository.deleteAll();

    userService.createUser(
            "Kevin",
            email,
            "encoded-password"
    );

    assertThat(userEntityRepository.findByEmail(email)).isPresent();

    UserDetails userDetails = org.springframework.security.core.userdetails.User
            .withUsername(email)
            .password("encoded-password")
            .roles("USER")
            .build();

    String token = jwtService.generateToken(userDetails);

    mockMvc.perform(MockMvcRequestBuilders.get("/allUsers")
                    .header("Authorization", "Bearer " + token))
            .andExpect(MockMvcResultMatchers.status().isOk());
    }
}
