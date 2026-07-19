package com.tex.cloud_task_manager.Organization.service;

import com.tex.cloud_task_manager.Organization.OrganizationEntity;
import com.tex.cloud_task_manager.Organization.OrganizationRepository;
import com.tex.cloud_task_manager.Organization.OrganizationUserEntity;
import com.tex.cloud_task_manager.Organization.OrganizationUserEntityRepository;
import com.tex.cloud_task_manager.Organization.response_request.OrgResponse;
import com.tex.cloud_task_manager.Security.CurrentUserService;
import com.tex.cloud_task_manager.common.exception.ResourceNotFoundException;
import com.tex.cloud_task_manager.common.exception.UnauthorizedException;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationServiceImpl implements OrganizationService {

  private final OrganizationRepository oRepository;

  private final OrganizationUserEntityRepository oUserEntityRepository;

  private final CurrentUserService cService;

  @Transactional
  @Override
  @Caching(
      evict = {
        @CacheEvict(cacheNames = "adminOrgs", key = "@currentUserService.getCurrentUserId()"),
        @CacheEvict(cacheNames = "userOrgs", key = "@currentUserService.getCurrentUserId()")
      })
  public OrgResponse createOrg(String name, String description) {

    long userId = cService.getCurrentUserId();
    log.debug("Creating organization for ownerId={}", userId);

    OrganizationEntity oEntity =
        OrganizationEntity.builder()
            .name(name)
            .description(description)
            .ownerId(userId)
            .createdAt(LocalDateTime.now())
            .build();

    var org = OrgResponse.from(oRepository.save(oEntity), null);

    OrganizationUserEntity oUserEntity =
        OrganizationUserEntity.builder().orgId(org.id()).userId(userId).build();
    oUserEntityRepository.save(oUserEntity);

    log.info("Organization created successfully with orgId={} for ownerId={}", org.id(), userId);
    return org;
  }

  @Override
  @Caching(
      evict = {
        @CacheEvict(cacheNames = "organizationById", allEntries = true),
        @CacheEvict(cacheNames = "adminOrgs", key = "@currentUserService.getCurrentUserId()"),
        @CacheEvict(cacheNames = "userOrgs", allEntries = true),
        @CacheEvict(cacheNames = "nonOrgUsers", allEntries = true)
      })
  public void deleteOrg(long id) {

    long userId = cService.getCurrentUserId();
    log.debug("Deleting organization with orgId={} for ownerId={}", id, userId);

    var org =
        oRepository
            .findByIdAndOwnerId(id, userId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException("Organization Not found for  %d".formatted(id)));

    oRepository.delete(org);
    log.info("Organization deleted successfully with orgId={} for ownerId={}", id, userId);
  }

  @Override
  @Caching(
      evict = {
        @CacheEvict(cacheNames = "organizationById", allEntries = true),
        @CacheEvict(cacheNames = "adminOrgs", key = "@currentUserService.getCurrentUserId()"),
        @CacheEvict(cacheNames = "userOrgs", allEntries = true),
        @CacheEvict(cacheNames = "nonOrgUsers", allEntries = true)
      })
  public void archiveOrg(long id) {
    log.warn("Organization archive requested for unimplemented operation with orgId={}", id);
    throw new UnsupportedOperationException("Unimplemented method 'archiveOrg'");
  }

  @Override
  @Cacheable(cacheNames = "adminOrgs", key = "@currentUserService.getCurrentUserId()")
  public List<OrgResponse> getAdminOrgs() {

    long userId = cService.getCurrentUserId();
    log.debug("Retrieving administered organizations for userId={}", userId);

    List<OrgResponse> orgList =
        oRepository.findByOwnerId(userId).stream()
            .map(
                org -> {
                  Long count = getOrgUserCount(org.getId());
                  return OrgResponse.from(org, count);
                })
            .toList();

    log.debug("Retrieved {} administered organizations for userId={}", orgList.size(), userId);
    return orgList;
  }

  @Override
  @Cacheable(cacheNames = "userOrgs", key = "@currentUserService.getCurrentUserId()")
  public List<OrgResponse> getUserOrgs() {

    long userId = cService.getCurrentUserId();
    log.debug("Retrieving joined organizations for userId={}", userId);

    List<OrgResponse> orgList =
        oRepository.findOrganizationsByUserId(userId).stream()
            .map(
                org -> {
                  Long count = getOrgUserCount(org.getId());
                  return OrgResponse.from(org, count);
                })
            .toList();

    log.debug("Retrieved {} joined organizations for userId={}", orgList.size(), userId);
    return orgList;
  }

  @Override
  @Caching(
      evict = {
        @CacheEvict(cacheNames = "organizationById", allEntries = true),
        @CacheEvict(cacheNames = "adminOrgs", key = "@currentUserService.getCurrentUserId()"),
        @CacheEvict(cacheNames = "userOrgs", key = "#userId"),
        @CacheEvict(cacheNames = "nonOrgUsers", key = "#orgId")
      })
  public void connectUserToOrganization(Long orgId, Long userId) {

    long currentUserId = cService.getCurrentUserId();
    log.debug("Connecting userId={} to orgId={} by ownerId={}", userId, orgId, currentUserId);

    oRepository
        .findByIdAndOwnerId(orgId, currentUserId)
        .orElseThrow(() -> new UnauthorizedException("No Org or No user match"));

    OrganizationUserEntity user =
        OrganizationUserEntity.builder().userId(userId).orgId(orgId).build();

    oUserEntityRepository.save(user);
    log.info(
        "User connected to organization successfully with userId={} and orgId={}", userId, orgId);
  }

  @Override
  @Cacheable(
      cacheNames = "organizationById",
      key = "@currentUserService.getCurrentUserId() + ':' + #orgId")
  public OrgResponse getOrganization(long orgId) {

    long currentUserId = cService.getCurrentUserId();
    log.debug("Retrieving organization with orgId={} for userId={}", orgId, currentUserId);

    OrganizationUserEntity oUserEntity =
        oUserEntityRepository
            .findByOrgIdAndUserId(orgId, currentUserId)
            .orElseThrow(
                () ->
                    new UnauthorizedException(
                        "No Org id %s or User id %s match".formatted(orgId, currentUserId)));
    Long id = oUserEntity.getOrgId();
    Optional<OrganizationEntity> org = oRepository.findById(id);

    if (!org.isPresent()) {
      log.warn("Organization lookup failed for orgId={} and userId={}", orgId, currentUserId);
      throw new UnauthorizedException("No Org or User match");
    }
    OrgResponse or = OrgResponse.from(org.get(), getOrgUserCount(id));

    log.debug("Retrieved organization with orgId={} for userId={}", orgId, currentUserId);
    return or;
  }

  private Long getOrgUserCount(Long orgId) {

    Long orgUserCount = oUserEntityRepository.getCountByOrgId(orgId);
    log.debug("Organization orgId={} has {} users", orgId, orgUserCount);
    return orgUserCount;
  }
}
