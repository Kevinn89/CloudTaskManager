package com.tex.cloud_task_manager.Security;

import com.tex.cloud_task_manager.User.UserEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final UserEntityRepository userEntityRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

    var user =
        userEntityRepository
            .findByEmail(username)
            .orElseThrow(
                () -> new UsernameNotFoundException("User not found with email: " + username));

    return User.builder()
        .username(user.getEmail())
        .password(user.getPassword())
        .roles("USER")
        .build();
  }
}
