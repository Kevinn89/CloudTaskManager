package com.tex.cloud_task_manager.Config;

import com.tex.cloud_task_manager.Project.ProjectEntity;
import com.tex.cloud_task_manager.Project.ProjectPriority;
import com.tex.cloud_task_manager.Project.ProjectRepository;
import com.tex.cloud_task_manager.Project.ProjectStatus;
import com.tex.cloud_task_manager.Task.TaskEntity;
import com.tex.cloud_task_manager.Task.TaskPriority;
import com.tex.cloud_task_manager.Task.TaskRepository;
import com.tex.cloud_task_manager.Task.TaskStatus;
import com.tex.cloud_task_manager.User.UserEntity;
import com.tex.cloud_task_manager.User.UserEntityRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class LocalDataSeeder implements CommandLineRunner {

  private static final int USER_COUNT = 1_000;
  private static final int PROJECT_COUNT = 10_000;
  private static final int TASK_COUNT = 10_000;

  private static final String SEED_PASSWORD = "ok";

  private final UserEntityRepository userRepository;
  private final ProjectRepository projectRepository;
  private final TaskRepository taskRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public void run(String... args) {
    String encodedPassword = passwordEncoder.encode(SEED_PASSWORD);
    LocalDateTime now = LocalDateTime.now();

    List<UserEntity> users = seedUsers(encodedPassword, now);
    List<ProjectEntity> projects = seedProjects(users, now);
    seedTasks(projects, now);
  }

  private List<UserEntity> seedUsers(String encodedPassword, LocalDateTime now) {
    List<UserEntity> users = new ArrayList<>(USER_COUNT);

    for (int i = 1; i <= USER_COUNT; i++) {
      users.add(
          UserEntity.builder()
              .name("Seed User " + i)
              .email("user" + i + "@test.com")
              .password(encodedPassword)
              .accountType(i == 1 ? "ADMIN" : "USER")
              .createdAt(now)
              .build());
    }

    return userRepository.saveAll(users);
  }

  private List<ProjectEntity> seedProjects(List<UserEntity> users, LocalDateTime now) {
    List<ProjectEntity> projects = new ArrayList<>(PROJECT_COUNT);
    ProjectPriority[] priorities = ProjectPriority.values();

    for (int i = 1; i <= PROJECT_COUNT; i++) {
      UserEntity owner = users.get((i - 1) % users.size());

      projects.add(
          ProjectEntity.builder()
              .userId(owner.getId())
              .name("Seed Project " + i)
              .description("Seeded project " + i)
              .createdAt(now)
              .status(i % 5 == 0 ? ProjectStatus.COMPLETED : ProjectStatus.ACTIVE)
              .priority(priorities[(i - 1) % priorities.length])
              .build());
    }

    return projectRepository.saveAll(projects);
  }

  private void seedTasks(List<ProjectEntity> projects, LocalDateTime now) {
    List<TaskEntity> tasks = new ArrayList<>(TASK_COUNT);
    TaskStatus[] statuses = TaskStatus.values();
    TaskPriority[] priorities = TaskPriority.values();

    for (int i = 1; i <= TASK_COUNT; i++) {
      ProjectEntity project = projects.get((i - 1) % projects.size());

      tasks.add(
          TaskEntity.builder()
              .project(project)
              .title("Seed Task " + i)
              .userId(project.getUserId())
              .description("Seeded task " + i)
              .taskStatus(statuses[(i - 1) % statuses.length])
              .createdAt(now)
              .completionDate(i % 4 == 0 ? LocalDate.now() : null)
              .dueDate(LocalDate.now().plusDays(i % 30))
              .priority(priorities[(i - 1) % priorities.length])
              .assignedUserid(project.getUserId())
              .build());
    }

    taskRepository.saveAll(tasks);
  }
}
