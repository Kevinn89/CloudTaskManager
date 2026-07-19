package com.tex.cloud_task_manager.Organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
    name = "organization_user",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_organization_user",
          columnNames = {"org_id", "user_id"})
    })
public class OrganizationUserEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  // @SequenceGenerator(name = "organizationuserentity_sequence", sequenceName =
  // "organizationuserentity_sequence", allocationSize = 100)
  private Long id;

  @Column(name = "org_id", nullable = false)
  private Long orgId;

  @Column(name = "user_id", nullable = false)
  private Long userId;
}
