package com.tex.cloud_task_manager.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@Entity
@Table(name = "users")
public class UserEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private  Long id;
        @Column(unique = true)
        private String email;
        @Column(nullable = false)
        private String password;
        @Column(nullable = false)
        private String name;
        @Column(nullable = false)
        private String createdAt;
        @Column(nullable = true)
        private String updatedAt;

}
