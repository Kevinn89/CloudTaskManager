package com.tex.cloud_task_manager.Config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
    String secret,
    long accessTokenExpirationMinutes,
    long refreshTokenExpirationDays
) {
    

}
