package com.tex.cloud_task_manager.Auth.response_request.Login_out;

import jakarta.validation.constraints.NotBlank;

public record LogOutRequest(
     @NotBlank String refreshToken
) {

}
