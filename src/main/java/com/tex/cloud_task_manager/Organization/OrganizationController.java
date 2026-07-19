package com.tex.cloud_task_manager.Organization;

import com.tex.cloud_task_manager.Organization.response_request.OrgCreateRequest;
import com.tex.cloud_task_manager.Organization.response_request.OrgResponse;
import com.tex.cloud_task_manager.Organization.service.OrganizationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organization")
@RequiredArgsConstructor
public class OrganizationController {

  private final OrganizationService orService;

  @PreAuthorize("hasAnyRole('ADMIN','USER')")
  @PostMapping("/create")
  public ResponseEntity<OrgResponse> create(@RequestBody OrgCreateRequest request) {

    OrgResponse response = orService.createOrg(request.name(), request.description());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PreAuthorize("hasAnyRole('ADMIN')")
  @PostMapping("/{orgId}/addUser/{userId}")
  public ResponseEntity<Void> addUserToOrg(
      @PathVariable("orgId") long orgId, @PathVariable("userId") long userId) {
    orService.connectUserToOrganization(orgId, userId);
    return ResponseEntity.noContent().build();
  }

  @PreAuthorize("hasAnyRole('ADMIN','USER')")
  @DeleteMapping("/{orgId}")
  public ResponseEntity<Void> delete(@PathVariable("orgId") long orgId) {
    orService.deleteOrg(orgId);
    return ResponseEntity.noContent().build();
  }

  // @PreAuthorize("hasAnyRole('ADMIN')")
  @GetMapping("/admin-orgs")
  public ResponseEntity<List<OrgResponse>> getAdminOrgs() {
    return ResponseEntity.ok(orService.getAdminOrgs());
  }

  @PreAuthorize("hasAnyRole('ADMIN','USER')")
  @GetMapping("/user-orgs")
  public ResponseEntity<List<OrgResponse>> getUserOrgs() {
    return ResponseEntity.ok(orService.getUserOrgs());
  }

  @PreAuthorize("hasAnyRole('ADMIN','USER')")
  @GetMapping("/{orgId}")
  public ResponseEntity<OrgResponse> getOrganization(@PathVariable("orgId") long orgId) {
    return ResponseEntity.ok(orService.getOrganization(orgId));
  }

  @PreAuthorize("hasAnyRole('ADMIN')")
  @PostMapping("/{id}/archive")
  public ResponseEntity<Void> archiveOrg(@PathVariable long id) {
    orService.archiveOrg(id);
    return ResponseEntity.noContent().build();
  }
}
