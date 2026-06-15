package com.tex.cloud_task_manager.Organization.response_request;

import java.time.LocalDateTime;

import com.tex.cloud_task_manager.Organization.OrganizationEntity;

public record OrgResponse(
                Long id,
                String name,
                String description,
                LocalDateTime createdAt,
                Long memberCount) {
        public static OrgResponse from(OrganizationEntity org, Long memberCount) {
                return new OrgResponse(
                                org.getId(),
                                org.getName(),
                                org.getDescription(),
                                org.getCreatedAt(),
                                memberCount);
        }

}