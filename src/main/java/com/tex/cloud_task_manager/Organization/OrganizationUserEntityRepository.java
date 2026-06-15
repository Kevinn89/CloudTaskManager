package com.tex.cloud_task_manager.Organization;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationUserEntityRepository extends JpaRepository<OrganizationUserEntity, Long> {

    Optional<OrganizationUserEntity> findByOrgIdAndUserId(Long orgId, Long userId);

    List<Optional<OrganizationUserEntity>> findByUserId(Long userId);

    @Query("""
                select count(*) as count_total
                from OrganizationUserEntity o
                where o.orgId = :orgId
            """)
    Long getCountByOrgId(@Param("orgId") Long orgId);

}
