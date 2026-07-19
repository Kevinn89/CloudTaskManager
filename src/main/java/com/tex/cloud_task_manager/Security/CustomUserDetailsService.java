package com.tex.cloud_task_manager.Security;

import com.tex.cloud_task_manager.User.UserEntityRepository;
import com.tex.cloud_task_manager.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

  private final UserEntityRepository userEntityRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    log.debug("Loading user details");

    var user =
        userEntityRepository
            .findByEmail(username)
            .orElseThrow(
                () ->
                    new UsernameNotFoundException(
                        "User not found with email: %s".formatted(username)));

    if (user.getVerifiedAt() == null) {
      throw new UnauthorizedException("User not verified for : %s".formatted(username));
    }

    UserDetails userDetails =
        User.builder()
            .username(user.getEmail())
            .password(user.getPassword())
            .roles(user.getAccountType())
            .build();
    log.debug("Loaded user details for userId={}", user.getId());
    return userDetails;
  }
}
