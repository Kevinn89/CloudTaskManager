package com.tex.cloud_task_manager.Auth.response_request;

public record RefreshTokenResponse(
    String accessToken
) implements AuthApiReponse {

}
