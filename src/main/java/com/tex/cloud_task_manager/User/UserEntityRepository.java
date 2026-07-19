package com.tex.cloud_task_manager.User;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserEntityRepository extends JpaRepository<UserEntity, Long> {

  Optional<UserEntity> findByEmail(String email);

  @Query(
      """

          select u
          from UserEntity u
          where not exists (
              select 1
              from OrganizationUserEntity ou
              where ou.userId = u.id
                and ou.orgId = :orgId
          )
      """)
  List<UserEntity> findUsersNotInOrganization(@Param("orgId") Long orgId);
}
