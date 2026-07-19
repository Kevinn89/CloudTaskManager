package com.tex.cloud_task_manager.Organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tex.cloud_task_manager.AbstractWebIntegrationTest;
import com.tex.cloud_task_manager.RefreshToken.RefreshTokenRepository;
import com.tex.cloud_task_manager.User.UserEntity;
import com.tex.cloud_task_manager.User.UserEntityRepository;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class OrganizationControllerIntegrationTest extends AbstractWebIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private OrganizationRepository organizationRepository;

  @Autowired private OrganizationUserEntityRepository organizationUserEntityRepository;

  @Autowired private UserEntityRepository userEntityRepository;

  @Autowired private RefreshTokenRepository refreshTokenRepository;

  private UserEntity owner;
  private UserEntity member;
  private UserEntity outsider;

  @BeforeEach
  void setUp() throws Exception {
    organizationUserEntityRepository.deleteAll();
    organizationRepository.deleteAll();
    refreshTokenRepository.deleteAll();
    userEntityRepository.deleteAll();

    registerUser("Owner", "owner@test.com", "Password123!", "ADMIN");
    registerUser("Member", "member@test.com", "Password123!", "USER");
    registerUser("Outsider", "outsider@test.com", "Password123!", "USER");

    owner = userEntityRepository.findByEmail("owner@test.com").orElseThrow();
    member = userEntityRepository.findByEmail("member@test.com").orElseThrow();
    outsider = userEntityRepository.findByEmail("outsider@test.com").orElseThrow();
  }

  @Test
  void createShouldPersistOrganizationAndCreatorMembership() throws Exception {
    String ownerToken = loginAndExtractAccessToken("owner@test.com", "Password123!");

    mockMvc
        .perform(
            post("/api/organization/create")
                .cookie(new Cookie("access_token", ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Owner Org",
                      "description": "Organization created through HTTP"
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.name").value("Owner Org"))
        .andExpect(jsonPath("$.description").value("Organization created through HTTP"));

    OrganizationEntity savedOrganization =
        organizationRepository.findAll().stream().findFirst().orElseThrow();

    assertThat(savedOrganization.getOwnerId()).isEqualTo(owner.getId());
    assertThat(savedOrganization.getCreatedAt()).isNotNull();

    assertThat(
            organizationUserEntityRepository.findByOrgIdAndUserId(
                savedOrganization.getId(), owner.getId()))
        .isPresent();
  }

  @Test
  void addUserShouldAllowMemberToReadOrganizationAndBlockNonMembers() throws Exception {
    String ownerToken = loginAndExtractAccessToken("owner@test.com", "Password123!");
    String memberToken = loginAndExtractAccessToken("member@test.com", "Password123!");
    String outsiderToken = loginAndExtractAccessToken("outsider@test.com", "Password123!");

    long orgId = createOrganization(ownerToken, "Team Org", "Shared team organization");

    mockMvc
        .perform(
            post("/api/organization/{orgId}/addUser/{userId}", orgId, member.getId())
                .cookie(new Cookie("access_token", ownerToken)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            get("/api/organization/{orgId}", orgId).cookie(new Cookie("access_token", memberToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(orgId))
        .andExpect(jsonPath("$.name").value("Team Org"))
        .andExpect(jsonPath("$.memberCount").value(2));

    mockMvc
        .perform(get("/api/organization/user-orgs").cookie(new Cookie("access_token", memberToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(orgId))
        .andExpect(jsonPath("$[0].memberCount").value(2));

    mockMvc
        .perform(
            get("/api/organization/{orgId}", orgId)
                .cookie(new Cookie("access_token", outsiderToken)))
        .andExpect(status().isUnauthorized())
        .andExpect(
            jsonPath("$.message")
                .value("No Org id %s or User id %s match".formatted(orgId, outsider.getId())));
  }

  @Test
  void adminOrgsShouldReturnOnlyOrganizationsOwnedByAuthenticatedUser() throws Exception {
    String ownerToken = loginAndExtractAccessToken("owner@test.com", "Password123!");
    String outsiderToken = loginAndExtractAccessToken("outsider@test.com", "Password123!");

    long ownerOrgId = createOrganization(ownerToken, "Owner Org", "Owned by admin");
    createOrganization(outsiderToken, "Outsider Org", "Owned by another user");

    mockMvc
        .perform(get("/api/organization/admin-orgs").cookie(new Cookie("access_token", ownerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(ownerOrgId))
        .andExpect(jsonPath("$[0].name").value("Owner Org"))
        .andExpect(jsonPath("$[0].memberCount").value(1));
  }

  private long createOrganization(String token, String name, String description) throws Exception {
    var result =
        mockMvc
            .perform(
                post("/api/organization/create")
                    .cookie(new Cookie("access_token", token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "name": "%s",
                          "description": "%s"
                        }
                        """
                            .formatted(name, description)))
            .andExpect(status().isCreated())
            .andReturn();

    return Long.parseLong(
        tools.jackson.databind.json.JsonMapper.builder()
            .build()
            .readTree(result.getResponse().getContentAsString())
            .get("id")
            .asText());
  }

  private void registerUser(String name, String email, String password, String accountType)
      throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "%s",
                      "email": "%s",
                      "password": "%s",
                      "accountType": "%s"
                    }
                    """
                        .formatted(name, email, password, accountType)))
        .andExpect(status().isOk());

    var user = userEntityRepository.findByEmail(email).orElseThrow();
    user.setVerifiedAt(Instant.now());
    userEntityRepository.save(user);
  }

  private String loginAndExtractAccessToken(String email, String password) throws Exception {
    var result =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "email": "%s",
                          "password": "%s"
                        }
                        """
                            .formatted(email, password)))
            .andExpect(status().isOk())
            .andReturn();

    return extractCookieValue(
        result.getResponse().getHeaders(HttpHeaders.SET_COOKIE), "access_token");
  }

  private static String extractCookieValue(List<String> setCookieHeaders, String cookieName) {
    return setCookieHeaders.stream()
        .filter(header -> header.startsWith(cookieName + "="))
        .map(header -> header.substring((cookieName + "=").length(), header.indexOf(';')))
        .findFirst()
        .orElseThrow();
  }
}
