package com.example.template.domain.refreshtoken.service;

import java.time.LocalDateTime;

public record RefreshTokenCacheValue(String tokenHash, LocalDateTime expiresAt) {

  public boolean isExpired(LocalDateTime now) {
    return expiresAt != null && expiresAt.isBefore(now);
  }
}
