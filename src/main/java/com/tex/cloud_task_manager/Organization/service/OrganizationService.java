package com.tex.cloud_task_manager.Organization.service;

import com.tex.cloud_task_manager.Organization.response_request.OrgResponse;
import java.util.List;

public interface OrganizationService {

  OrgResponse createOrg(String name, String description);

  void deleteOrg(long id);

  void archiveOrg(long id);

  List<OrgResponse> getAdminOrgs();

  List<OrgResponse> getUserOrgs();

  void connectUserToOrganization(Long orgId, Long userId);

  OrgResponse getOrganization(long orgId);

  // int getOrgUserCount(Long orgId);

}
