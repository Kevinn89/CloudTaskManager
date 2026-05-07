package com.tex.cloud_task_manager.Project;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {

        List<ProjectEntity> findByUserId(long userId);

        Optional<ProjectEntity>  findByIdAndUserId(long projectId, long userId);
}
