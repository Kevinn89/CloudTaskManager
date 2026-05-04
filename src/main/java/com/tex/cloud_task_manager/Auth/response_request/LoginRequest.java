package com.tex.cloud_task_manager.Auth.response_request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest( 
       @Email String email,
       @NotBlank String password,
       String login_dt
) {

}
