package com.tex.cloud_task_manager.Auth.response_request;

import jakarta.validation.constraints.NotBlank;

public record LogOutRequest(
     @NotBlank String refreshToken
) {

}
