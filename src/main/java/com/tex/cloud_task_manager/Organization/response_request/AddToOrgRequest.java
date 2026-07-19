package com.tex.cloud_task_manager.Organization.response_request;

import jakarta.validation.constraints.NotBlank;

public record AddToOrgRequest(@NotBlank long userId) {}
