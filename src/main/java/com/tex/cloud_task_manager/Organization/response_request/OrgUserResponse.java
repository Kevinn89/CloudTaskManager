package com.tex.cloud_task_manager.Organization.response_request;

public record OrgUserResponse(
        Long orgId,
        Long userId,
        String name,
        Long memberCount

) {
    public static OrgUserResponse from(Long orgId, Long userId, String name, Long memberLong) {

        return new OrgUserResponse(orgId, userId, name, memberLong);
    }

}
