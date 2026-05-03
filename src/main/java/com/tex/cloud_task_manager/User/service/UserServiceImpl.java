package com.tex.cloud_task_manager.User.service;

import java.time.Instant;
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
                .createdAt(Instant.now().toString())
                .updatedAt(null)
                .build();
        return userEntityRepository.save(user);
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

    @Override
    public UserEntity updateUser(Long id, String name, String email, String password) {
        
        UserEntity user = UserEntity.builder()
                .id(id)
                .email(email)
                .name(name)
                .password(password)  // need to hash
                .build();
                
        return userEntityRepository.save(user);
    }

    @Override
    public List<UserEntity> getAllUsers() {
       return userEntityRepository.findAll();
    }


}
