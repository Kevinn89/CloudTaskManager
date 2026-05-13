package com.tex.cloud_task_manager.Security;

import com.tex.cloud_task_manager.User.UserEntityRepository;
import com.tex.cloud_task_manager.common.exception.ResourceNotFoundException;
import com.tex.cloud_task_manager.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

  private final UserEntityRepository userRepository;

  public Long getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      throw new ResourceNotFoundException("No authenticated user found");
    }

    Object principal = authentication.getPrincipal();

    if (!(principal instanceof UserDetails userDetails)) {
      throw new UnauthorizedException("Invalid authenticated principal");
    }

    return userRepository
        .findByEmail(userDetails.getUsername())
        .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"))
        .getId();
  }
}
