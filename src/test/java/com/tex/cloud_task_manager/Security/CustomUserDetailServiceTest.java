package com.tex.cloud_task_manager.Security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.tex.cloud_task_manager.User.UserEntity;
import com.tex.cloud_task_manager.User.UserEntityRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
public class CustomUserDetailServiceTest {

  @Mock private UserEntityRepository userEntityRepository;

  @InjectMocks private CustomUserDetailsService customService;

  private UserEntity userEntity;

  @BeforeEach
  void setUp() {
    userEntity =
        UserEntity.builder()
            .id(1L)
            .name("Kevin")
            .email("test@example.com")
            .password("password123")
            .createdAt(LocalDateTime.now())
            .build();
  }

  @Test
  public void testLoadUserByUsername_success() {

    when(userEntityRepository.findByEmail("test@example.com")).thenReturn(Optional.of(userEntity));

    UserDetails userDetails = customService.loadUserByUsername("test@example.com");

    assertEquals(userDetails.getPassword(), "password123");
    assertEquals(userDetails.getUsername(), "test@example.com");
  }

  @Test
  public void testLoadUserByUsername_UserNotFound() {

    when(userEntityRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

    UsernameNotFoundException exception =
        assertThrows(
            UsernameNotFoundException.class,
            () -> customService.loadUserByUsername("test@example.com"));

    assertEquals("User not found with email: test@example.com", exception.getMessage());
  }
}
