package com.tex.cloud_task_manager.Config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class CorsConfigTest {

  @Test
  void allowsDockerComposeFrontendToCallLogout() {
    CorsConfigurationSource source = new CorsConfig().corsConfigurationSource();
    MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/auth/logout");

    CorsConfiguration configuration = source.getCorsConfiguration(request);

    assertThat(configuration).isNotNull();
    assertThat(configuration.getAllowedOrigins())
        .contains("http://localhost:8081", "http://127.0.0.1:8081");
    assertThat(configuration.getAllowedMethods()).contains("POST", "OPTIONS");
    assertThat(configuration.getAllowCredentials()).isTrue();
  }
}
