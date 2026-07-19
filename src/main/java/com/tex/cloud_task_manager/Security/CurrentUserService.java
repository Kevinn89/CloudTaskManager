package com.tex.cloud_task_manager.Security;

import com.tex.cloud_task_manager.User.UserEntityRepository;
import com.tex.cloud_task_manager.common.exception.ResourceNotFoundException;
import com.tex.cloud_task_manager.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrentUserService {

  private final UserEntityRepository userRepository;

  public Long getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      log.warn("Current user lookup failed because no authenticated user was found");
      throw new ResourceNotFoundException("No authenticated user found");
    }

    Object principal = authentication.getPrincipal();

    if (!(principal instanceof UserDetails userDetails)) {
      log.warn("Current user lookup failed because the authenticated principal is invalid");
      throw new UnauthorizedException("Invalid authenticated principal");
    }

    Long userId =
        userRepository
            .findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"))
            .getId();
    log.debug("Resolved current userId={}", userId);
    return userId;
  }
}
