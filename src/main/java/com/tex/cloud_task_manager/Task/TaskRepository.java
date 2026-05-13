package com.tex.cloud_task_manager.Task;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<TaskEntity, Long> {

  List<TaskEntity> findByUserId(Long userId);

  TaskEntity findByIdAndUserId(Long userId, Long long1);

  List<TaskEntity> findByProjectIdAndUserId(long projectId, Long userId);

  Optional<TaskEntity> findByIdAndProjectIdAndUserId(Long taskId, Long projectId, long userId);
}
