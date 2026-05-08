package com.tex.cloud_task_manager.RefreshToken.response_request;

import com.tex.cloud_task_manager.Auth.response_request.AuthApiReponse;

public record RefreshTokenResponse(
    String accessToken
) implements AuthApiReponse {

}
