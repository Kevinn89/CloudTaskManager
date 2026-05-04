package com.tex.cloud_task_manager.User;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserEntityRepository extends JpaRepository<UserEntity, Long> {

  Optional<UserEntity> findByEmail(String email);


  @Modifying
  @Query("""
      update UserEntity u
      set u.loginDt = CURRENT_TIMESTAMP
      where u.id = :userId
  """)
  int updateLoginDtById(@Param("userId") Long userId);

}
