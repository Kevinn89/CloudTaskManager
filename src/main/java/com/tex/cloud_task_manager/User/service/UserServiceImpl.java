package com.tex.cloud_task_manager.User.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.tex.cloud_task_manager.User.UserEntity;
import com.tex.cloud_task_manager.User.UserEntityRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserEntityRepository userEntityRepository;

    @Override
    public UserEntity createUser(String name, String email, String password) {
        UserEntity user = UserEntity.builder()
                .name(name)
                .email(email)
                .password(password)
                .createdAt(LocalDateTime.now())
                .updatedAt(null)
                .build();
        return userEntityRepository.save(user);
    }

    @Override
    public List<UserEntity> getAllUsers() {
       return userEntityRepository.findAll();
    }
    
}
