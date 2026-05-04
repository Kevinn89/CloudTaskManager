package com.tex.cloud_task_manager.Security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.tex.cloud_task_manager.User.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
         
        var user = userService.findByEmail(username);

        if(!user.isPresent()) {
              throw new UsernameNotFoundException("User not found");
          }
        userService.updateLoginDt(user.get().getId());
            return org.springframework.security.core.userdetails.User
                .withUsername(user.get().getEmail())
                .password(user.get().getPassword())
                .roles("USER")
                .build();
    }

}
