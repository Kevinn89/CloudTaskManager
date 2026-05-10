package com.tex.cloud_task_manager.Project;


import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "projects")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        @Column(name = "user_id", nullable = false)
        private Long userId;
        @Column(name = "name", nullable = false)
        private String name;
        @Column(name = "description", length = 500)
        private String description;
        @Column(name = "created_at", nullable = false, updatable = false)
        private LocalDateTime createdAt;
        @Column(name = "updated_at", nullable = true)
        private LocalDateTime updatedAt;
        @Column(name = "status", nullable = false)
        private ProjectStatus status;

}
