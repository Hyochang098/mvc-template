package com.example.template.global.security.service;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

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
    String hashedToken = hashToken(token);
    if (Boolean.TRUE.equals(accessTokenBlacklistCache.getIfPresent(hashedToken))) {
      return true;
    }
    String value = stringRedisTemplate.opsForValue().get(buildKey(hashedToken));
    if (value != null) {
      accessTokenBlacklistCache.put(hashedToken, Boolean.TRUE);
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

    String hashedToken = hashToken(token);
    accessTokenBlacklistCache.put(hashedToken, Boolean.TRUE);
    stringRedisTemplate.opsForValue()
        .set(buildKey(hashedToken), "1", Duration.ofSeconds(ttlToUse));
  }

  private String buildKey(String hashedToken) {
    return REDIS_KEY_PREFIX + hashedToken;
  }

  private String hashToken(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }
}
