package com.tex.cloud_task_manager.Organization.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.tex.cloud_task_manager.Organization.OrganizationEntity;
import com.tex.cloud_task_manager.Organization.OrganizationRepository;
import com.tex.cloud_task_manager.Organization.OrganizationUserEntity;
import com.tex.cloud_task_manager.Organization.OrganizationUserEntityRepository;
import com.tex.cloud_task_manager.Organization.response_request.OrgResponse;
import com.tex.cloud_task_manager.Security.CurrentUserService;
import com.tex.cloud_task_manager.common.exception.ResourceNotFoundException;
import com.tex.cloud_task_manager.common.exception.UnauthorizedException;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository oRepository;

    private final OrganizationUserEntityRepository oUserEntityRepository;

    private final CurrentUserService cService;

    @Transactional
    @Override
    public OrgResponse createOrg(String name, String description) {

        long userId = cService.getCurrentUserId();

        OrganizationEntity oEntity = OrganizationEntity.builder()
                .name(name)
                .description(description)
                .ownerId(userId)
                .createdAt(LocalDateTime.now())
                .build();

        var org = OrgResponse.from(oRepository.save(oEntity), null);

        OrganizationUserEntity oUserEntity = OrganizationUserEntity.builder().orgId(org.id()).userId(userId).build();
        oUserEntityRepository.save(oUserEntity);

        return org;
    }

    @Transactional
    @Override
    public void deleteOrg(long id) {

        long userId = cService.getCurrentUserId();

        var org = oRepository.findByIdAndOwnerId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization Not found for  %d".formatted(id)));

        oRepository.delete(org);
    }

    @Override
    public void archiveOrg(long id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'archiveOrg'");
    }

    @Transactional
    @Override
    public List<OrgResponse> getAdminOrgs() {

        long userId = cService.getCurrentUserId();

        List<OrgResponse> orgList = oRepository.findByOwnerId(userId).stream().map(org -> {
            Long count = getOrgUserCount(org.getId());
            return OrgResponse.from(org, count);
        }).toList();

        return orgList;
    }

    @Transactional
    @Override
    public List<OrgResponse> getUserOrgs() {

        long userId = cService.getCurrentUserId();

        List<OrgResponse> orgList = oRepository.findOrganizationsByUserId(userId)
                .stream().map(org -> {
                    Long count = getOrgUserCount(org.getId());
                    return OrgResponse.from(org, count);
                }).toList();

        return orgList;
    }

    @Transactional
    @Override
    public void connectUserToOrganization(Long orgId, Long userId) {

        long currentUserId = cService.getCurrentUserId();

        oRepository.findByIdAndOwnerId(orgId, currentUserId)
                .orElseThrow(() -> new UnauthorizedException("No Org or No user match"));

        OrganizationUserEntity user = OrganizationUserEntity.builder().userId(userId).orgId(orgId).build();

        oUserEntityRepository.save(user);
    }

    @Transactional
    @Override
    public OrgResponse getOrganization(long orgId) {

        long currentUserId = cService.getCurrentUserId();

        OrganizationUserEntity oUserEntity = oUserEntityRepository.findByOrgIdAndUserId(orgId, currentUserId)
                .orElseThrow(() -> new UnauthorizedException(
                        "No Org id %s or User id %s match".formatted(orgId, currentUserId)));
        Long id = oUserEntity.getOrgId();
        Optional<OrganizationEntity> org = oRepository.findById(id);

        if (!org.isPresent()) {
            throw new UnauthorizedException("No Org or User match");
        }
        OrgResponse or = OrgResponse.from(org.get(), getOrgUserCount(id));

        return or;
    }

    private Long getOrgUserCount(Long orgId) {

        Long orgUserCount = oUserEntityRepository.getCountByOrgId(orgId);
        return orgUserCount;
    }

}
