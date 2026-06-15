package com.tex.cloud_task_manager.Config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.tex.cloud_task_manager.Security.CustomUserDetailsService;
import com.tex.cloud_task_manager.Security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

  @Mock private JwtService jwtService;

  @Mock private CustomUserDetailsService userDetailsService;

  @Mock private FilterChain filterChain;

  private JwtAuthFilter jwtAuthFilter;

  @BeforeEach
  void setUp() {
    jwtAuthFilter = new JwtAuthFilter(jwtService, userDetailsService);
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldNotFilterShouldReturnTrueForAuthEndpoints() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setServletPath("/api/auth/login");

    boolean result = jwtAuthFilter.shouldNotFilter(request);

    assertThat(result).isTrue();
  }

  @Test
  void shouldNotFilterShouldReturnTrueForH2Console() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setServletPath("/h2-console");

    boolean result = jwtAuthFilter.shouldNotFilter(request);

    assertThat(result).isTrue();
  }

  @Test
  void shouldNotFilterShouldReturnFalseForProtectedEndpoint() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setServletPath("/api/users");

    boolean result = jwtAuthFilter.shouldNotFilter(request);

    assertThat(result).isFalse();
  }

  @Test
  void doFilterInternalShouldContinueFilterChainWhenAccessTokenCookieIsMissing()
      throws ServletException, IOException {

    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verifyNoInteractions(jwtService);
    verifyNoInteractions(userDetailsService);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void doFilterInternalShouldIgnoreAuthorizationHeaderWhenAccessTokenCookieIsMissing()
      throws ServletException, IOException {

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer abc123");

    MockHttpServletResponse response = new MockHttpServletResponse();

    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verifyNoInteractions(jwtService);
    verifyNoInteractions(userDetailsService);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void doFilterInternalShouldSetAuthenticationWhenAccessTokenCookieIsValid()
      throws ServletException, IOException {

    String token = "valid-jwt-token";
    String email = "kevin@test.com";

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie("access_token", token));

    MockHttpServletResponse response = new MockHttpServletResponse();

    UserDetails userDetails =
        org.springframework.security.core.userdetails.User.withUsername(email)
            .password("encoded-password")
            .roles("USER")
            .build();

    when(jwtService.extractUsername(token)).thenReturn(email);
    when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
    when(jwtService.isTokenValid(token, userDetails)).thenReturn(true);

    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    var authentication = SecurityContextHolder.getContext().getAuthentication();

    assertThat(authentication).isNotNull();
    assertThat(authentication.getPrincipal()).isEqualTo(userDetails);
    assertThat(authentication.getCredentials()).isNull();
    assertThat(authentication.getAuthorities())
        .extracting(authority -> authority.getAuthority())
        .containsExactly("ROLE_USER");

    verify(jwtService).extractUsername(token);
    verify(userDetailsService).loadUserByUsername(email);
    verify(jwtService).isTokenValid(token, userDetails);
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilterInternalShouldNotSetAuthenticationWhenTokenIsInvalid()
      throws ServletException, IOException {

    String token = "invalid-jwt-token";
    String email = "kevin@test.com";

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie("access_token", token));

    MockHttpServletResponse response = new MockHttpServletResponse();

    UserDetails userDetails =
        org.springframework.security.core.userdetails.User.withUsername(email)
            .password("encoded-password")
            .roles("USER")
            .build();

    when(jwtService.extractUsername(token)).thenReturn(email);
    when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
    when(jwtService.isTokenValid(token, userDetails)).thenReturn(false);

    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

    verify(jwtService).extractUsername(token);
    verify(userDetailsService).loadUserByUsername(email);
    verify(jwtService).isTokenValid(token, userDetails);
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilterInternalShouldNotLoadUserWhenAuthenticationAlreadyExists()
      throws ServletException, IOException {

    String token = "valid-jwt-token";
    String email = "kevin@test.com";

    UserDetails existingUser =
        org.springframework.security.core.userdetails.User.withUsername("existing@test.com")
            .password("encoded-password")
            .roles("USER")
            .build();

    var existingAuth =
        new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            existingUser, null, existingUser.getAuthorities());

    SecurityContextHolder.getContext().setAuthentication(existingAuth);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie("access_token", token));

    MockHttpServletResponse response = new MockHttpServletResponse();

    when(jwtService.extractUsername(token)).thenReturn(email);

    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(existingAuth);

    verify(jwtService).extractUsername(token);
    verifyNoInteractions(userDetailsService);
    verify(jwtService, never()).isTokenValid(anyString(), any());
    verify(filterChain).doFilter(request, response);
  }
}
