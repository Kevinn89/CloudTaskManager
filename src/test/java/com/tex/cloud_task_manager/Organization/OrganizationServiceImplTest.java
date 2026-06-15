package com.tex.cloud_task_manager.Organization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tex.cloud_task_manager.Organization.response_request.OrgResponse;
import com.tex.cloud_task_manager.Organization.service.OrganizationServiceImpl;
import com.tex.cloud_task_manager.Security.CurrentUserService;
import com.tex.cloud_task_manager.common.exception.ResourceNotFoundException;
import com.tex.cloud_task_manager.common.exception.UnauthorizedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceImplTest {

  @Mock private OrganizationRepository organizationRepository;

  @Mock private OrganizationUserEntityRepository organizationUserEntityRepository;

  @Mock private CurrentUserService currentUserService;

  @InjectMocks private OrganizationServiceImpl organizationService;

  private OrganizationEntity ownedOrganization;
  private OrganizationEntity joinedOrganization;

  @BeforeEach
  void setUp() {
    ownedOrganization =
        OrganizationEntity.builder()
            .id(1L)
            .name("Owner Org")
            .description("Owned by current user")
            .ownerId(10L)
            .createdAt(LocalDateTime.parse("2026-06-01T10:00:00"))
            .build();

    joinedOrganization =
        OrganizationEntity.builder()
            .id(2L)
            .name("Joined Org")
            .description("Current user is a member")
            .ownerId(20L)
            .createdAt(LocalDateTime.parse("2026-06-02T10:00:00"))
            .build();
  }

  @Test
  void createOrgShouldSaveOrganizationAndAddCreatorAsMember() {
    when(currentUserService.getCurrentUserId()).thenReturn(10L);
    when(organizationRepository.save(any(OrganizationEntity.class)))
        .thenAnswer(
            invocation -> {
              OrganizationEntity org = invocation.getArgument(0);
              org.setId(1L);
              return org;
            });

    OrgResponse response = organizationService.createOrg("Owner Org", "Owned by current user");

    ArgumentCaptor<OrganizationEntity> organizationCaptor =
        ArgumentCaptor.forClass(OrganizationEntity.class);
    verify(organizationRepository).save(organizationCaptor.capture());

    OrganizationEntity organizationToSave = organizationCaptor.getValue();
    assertEquals("Owner Org", organizationToSave.getName());
    assertEquals("Owned by current user", organizationToSave.getDescription());
    assertEquals(10L, organizationToSave.getOwnerId());

    ArgumentCaptor<OrganizationUserEntity> membershipCaptor =
        ArgumentCaptor.forClass(OrganizationUserEntity.class);
    verify(organizationUserEntityRepository).save(membershipCaptor.capture());

    OrganizationUserEntity membershipToSave = membershipCaptor.getValue();
    assertEquals(1L, membershipToSave.getOrgId());
    assertEquals(10L, membershipToSave.getUserId());

    assertEquals(1L, response.id());
    assertEquals("Owner Org", response.name());
    assertEquals("Owned by current user", response.description());
  }

  @Test
  void getAdminOrgsShouldReturnOnlyOrganizationsOwnedByCurrentUserWithMemberCounts() {
    when(currentUserService.getCurrentUserId()).thenReturn(10L);
    when(organizationRepository.findByOwnerId(10L)).thenReturn(List.of(ownedOrganization));
    when(organizationUserEntityRepository.getCountByOrgId(1L)).thenReturn(3L);

    List<OrgResponse> responses = organizationService.getAdminOrgs();

    assertEquals(1, responses.size());
    assertEquals(1L, responses.getFirst().id());
    assertEquals("Owner Org", responses.getFirst().name());
    assertEquals(3L, responses.getFirst().memberCount());

    verify(organizationRepository).findByOwnerId(10L);
  }

  @Test
  void getUserOrgsShouldReturnMembershipOrganizationsWithMemberCounts() {
    when(currentUserService.getCurrentUserId()).thenReturn(10L);
    when(organizationRepository.findOrganizationsByUserId(10L))
        .thenReturn(List.of(joinedOrganization));
    when(organizationUserEntityRepository.getCountByOrgId(2L)).thenReturn(2L);

    List<OrgResponse> responses = organizationService.getUserOrgs();

    assertEquals(1, responses.size());
    assertEquals(2L, responses.getFirst().id());
    assertEquals("Joined Org", responses.getFirst().name());
    assertEquals(2L, responses.getFirst().memberCount());

    verify(organizationRepository).findOrganizationsByUserId(10L);
  }

  @Test
  void connectUserToOrganizationShouldRequireCurrentUserToOwnOrganization() {
    when(currentUserService.getCurrentUserId()).thenReturn(10L);
    when(organizationRepository.findByIdAndOwnerId(1L, 10L))
        .thenReturn(Optional.of(ownedOrganization));

    organizationService.connectUserToOrganization(1L, 20L);

    ArgumentCaptor<OrganizationUserEntity> membershipCaptor =
        ArgumentCaptor.forClass(OrganizationUserEntity.class);
    verify(organizationUserEntityRepository).save(membershipCaptor.capture());

    OrganizationUserEntity membershipToSave = membershipCaptor.getValue();
    assertEquals(1L, membershipToSave.getOrgId());
    assertEquals(20L, membershipToSave.getUserId());
  }

  @Test
  void connectUserToOrganizationShouldThrowWhenCurrentUserDoesNotOwnOrganization() {
    when(currentUserService.getCurrentUserId()).thenReturn(10L);
    when(organizationRepository.findByIdAndOwnerId(1L, 10L)).thenReturn(Optional.empty());

    UnauthorizedException exception =
        assertThrows(
            UnauthorizedException.class,
            () -> organizationService.connectUserToOrganization(1L, 20L));

    assertEquals("No Org or No user match", exception.getMessage());
    verify(organizationUserEntityRepository, never()).save(any());
  }

  @Test
  void getOrganizationShouldRequireCurrentUserMembership() {
    when(currentUserService.getCurrentUserId()).thenReturn(10L);
    when(organizationUserEntityRepository.findByOrgIdAndUserId(2L, 10L))
        .thenReturn(Optional.of(OrganizationUserEntity.builder().orgId(2L).userId(10L).build()));
    when(organizationRepository.findById(2L)).thenReturn(Optional.of(joinedOrganization));
    when(organizationUserEntityRepository.getCountByOrgId(2L)).thenReturn(2L);

    OrgResponse response = organizationService.getOrganization(2L);

    assertEquals(2L, response.id());
    assertEquals("Joined Org", response.name());
    assertEquals(2L, response.memberCount());
  }

  @Test
  void deleteOrgShouldRequireCurrentUserToOwnOrganization() {
    when(currentUserService.getCurrentUserId()).thenReturn(10L);
    when(organizationRepository.findByIdAndOwnerId(1L, 10L)).thenReturn(Optional.empty());

    ResourceNotFoundException exception =
        assertThrows(ResourceNotFoundException.class, () -> organizationService.deleteOrg(1L));

    assertEquals("Organization Not found for  1", exception.getMessage());
    verify(organizationRepository, never()).delete(any());
  }
}
