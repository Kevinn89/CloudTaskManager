package com.tex.cloud_task_manager.Organization;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationRepository extends JpaRepository<OrganizationEntity, Long> {

  Optional<OrganizationEntity> findByIdAndOwnerId(long id, long ownerId);

  List<OrganizationEntity> findByOwnerId(Long ownerId);

  @Query(
      """
          select o
          from OrganizationEntity o
          join OrganizationUserEntity ou
            on ou.orgId = o.id
          where ou.userId = :userId
          and o.ownerId <> :userId
      """)
  List<OrganizationEntity> findOrganizationsByUserId(@Param("userId") Long userId);
}
