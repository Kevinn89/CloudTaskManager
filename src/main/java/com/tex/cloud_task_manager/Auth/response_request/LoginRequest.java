package com.tex.cloud_task_manager.Auth.response_request;

public record LoginRequest(
    
        String email,
        String password,
        String login_dt

) {

}
