package com.tex.cloud_task_manager.RefreshToken.service;

import com.tex.cloud_task_manager.RefreshToken.RefreshTokenEntity;

public interface RefreshTokenService {

  RefreshTokenEntity generateRefreshToken(String email);

  void revokeRefreshToken(String token);

  RefreshTokenEntity getRefreshTokenNotRevoked(String token);
}
