package com.tex.cloud_task_manager.User.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
        return saveUserEntity(user);
    }

    @Override
    public Optional<UserEntity> getUserById(Long id) {
        return userEntityRepository.findById(id);
    }

    @Override
    public void deleteUser(Long id) {
        userEntityRepository.deleteById(id);
    }

    @Override
    public Optional<UserEntity> findByEmail(String email) {
        return userEntityRepository.findByEmail(email);
    }

    private UserEntity saveUserEntity(UserEntity user) {
        return userEntityRepository.save(user);
    }

    @Override
    public List<UserEntity> getAllUsers() {
       return userEntityRepository.findAll();
    }
    
}
