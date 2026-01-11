package com.example.template.global.security.service;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccessTokenBlacklistService {

  private static final String REDIS_KEY_PREFIX = "blacklist:access:";

  private final Cache<String, Boolean> accessTokenBlacklistCache;
  private final StringRedisTemplate stringRedisTemplate;

  @Value("${cache.access-token-blacklist.ttl-seconds:0}")
  private long defaultBlacklistTtlSeconds;

  public boolean isBlacklisted(String token) {
    if (!StringUtils.hasText(token)) {
      return false;
    }
    if (Boolean.TRUE.equals(accessTokenBlacklistCache.getIfPresent(token))) {
      return true;
    }
    Boolean exists = stringRedisTemplate.hasKey(buildKey(token));
    if (Boolean.TRUE.equals(exists)) {
      accessTokenBlacklistCache.put(token, Boolean.TRUE);
      return true;
    }
    return false;
  }

  public void blacklist(String token, long ttlSeconds) {
    if (!StringUtils.hasText(token)) {
      return;
    }

    long ttlToUse = ttlSeconds > 0 ? ttlSeconds : defaultBlacklistTtlSeconds;
    if (ttlToUse <= 0) {
      log.debug("[AccessTokenBlacklist] TTL이 0 이하이므로 블랙리스트에 저장하지 않습니다.");
      return;
    }

    accessTokenBlacklistCache.put(token, Boolean.TRUE);
    stringRedisTemplate.opsForValue()
        .set(buildKey(token), "1", Duration.ofSeconds(ttlToUse));
  }

  private String buildKey(String token) {
    return REDIS_KEY_PREFIX + token;
  }
}
