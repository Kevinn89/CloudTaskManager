package com.tex.cloud_task_manager.Task;



import java.time.LocalDate;
import java.time.LocalDateTime;

import com.tex.cloud_task_manager.Project.ProjectEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tasks")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "project_id", nullable = false)
        private ProjectEntity project;
        
        @Column(name = "title", nullable = false)
        private String title;

        @Column(nullable = true)
        private Long userId;
        @Column(name = "description")
        private String description;
        @Column(name = "status", nullable = false)
        private TaskStatus taskStatus;
        @Column(nullable = false)
        private LocalDateTime createdAt;
        @Column(nullable = true)
        private LocalDateTime updatedAt;
        @Column(nullable = true)
        private LocalDate completionDate;
        @Column(nullable = true)
        private LocalDate dueDate;

        @Column(name = "project_priority", nullable = true)
        private TaskPriority priority;

}