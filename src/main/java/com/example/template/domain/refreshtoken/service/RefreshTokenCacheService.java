package com.example.template.domain.refreshtoken.service;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCacheService {

  private static final String REDIS_KEY_PREFIX = "refreshToken:";

  private final Cache<Long, RefreshTokenCacheValue> refreshTokenCache;
  private final StringRedisTemplate stringRedisTemplate;

  @Value("${cache.refresh-token.ttl-seconds:900}")
  private long refreshTokenTtlSeconds;

  public Optional<RefreshTokenCacheValue> get(Long userId) {
    RefreshTokenCacheValue cached = refreshTokenCache.getIfPresent(userId);
    if (cached != null) {
      if (cached.isExpired(LocalDateTime.now())) {
        evict(userId);
        return Optional.empty();
      }
      return Optional.of(cached);
    }

    String raw = stringRedisTemplate.opsForValue().get(buildKey(userId));
    if (!StringUtils.hasText(raw)) {
      return Optional.empty();
    }

    RefreshTokenCacheValue value = deserialize(raw);
    if (value == null || value.isExpired(LocalDateTime.now())) {
      evict(userId);
      return Optional.empty();
    }

    refreshTokenCache.put(userId, value);
    return Optional.of(value);
  }

  public void store(Long userId, String tokenHash, LocalDateTime expiresAt) {
    RefreshTokenCacheValue cacheValue = new RefreshTokenCacheValue(tokenHash, expiresAt);
    refreshTokenCache.put(userId, cacheValue);
    if (refreshTokenTtlSeconds > 0) {
      stringRedisTemplate.opsForValue()
          .set(buildKey(userId), serialize(cacheValue), Duration.ofSeconds(refreshTokenTtlSeconds));
    }
  }

  public void evict(Long userId) {
    refreshTokenCache.invalidate(userId);
    stringRedisTemplate.delete(buildKey(userId));
  }

  private String buildKey(Long userId) {
    return REDIS_KEY_PREFIX + userId;
  }

  private String serialize(RefreshTokenCacheValue value) {
    return value.tokenHash() + "|" + value.expiresAt();
  }

  private RefreshTokenCacheValue deserialize(String raw) {
    String[] parts = raw.split("\\|", 2);
    if (parts.length != 2) {
      log.warn("[RefreshTokenCache] Deserialize 실패 - raw={}", raw);
      return null;
    }
    try {
      return new RefreshTokenCacheValue(parts[0], LocalDateTime.parse(parts[1]));
    } catch (Exception ex) {
      log.warn("[RefreshTokenCache] Deserialize 예외 - raw={}, ex={}", raw, ex.getMessage());
      return null;
    }
  }
}
