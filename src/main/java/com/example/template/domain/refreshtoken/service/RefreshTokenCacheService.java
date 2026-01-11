package com.example.template.domain.refreshtoken.service;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCacheService {

  private static final String REDIS_KEY_PREFIX = "refreshToken:";
  private static final String HASH_FIELD = "hash";
  private static final String EXP_FIELD = "exp";

  private final Cache<Long, RefreshTokenCacheValue> refreshTokenCache;
  private final StringRedisTemplate stringRedisTemplate;
  private final ObjectMapper objectMapper = new ObjectMapper();

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
    String serialized = serialize(cacheValue);
    if (refreshTokenTtlSeconds > 0 && serialized != null) {
      stringRedisTemplate.opsForValue()
          .set(buildKey(userId), serialized, Duration.ofSeconds(refreshTokenTtlSeconds));
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
    String encodedHash = Base64.getEncoder().encodeToString(value.tokenHash().getBytes(StandardCharsets.UTF_8));
    try {
      return objectMapper.writeValueAsString(
          Map.of(
              HASH_FIELD, encodedHash,
              EXP_FIELD, value.expiresAt().toString()
          )
      );
    } catch (JsonProcessingException e) {
      log.warn("[RefreshTokenCache] 직렬화 실패");
      return null;
    }
  }

  private RefreshTokenCacheValue deserialize(String raw) {
    try {
      Map<String, String> payload = objectMapper.readValue(raw, new TypeReference<>() {});
      String encodedHash = payload.get(HASH_FIELD);
      String expires = payload.get(EXP_FIELD);
      if (!StringUtils.hasText(encodedHash) || !StringUtils.hasText(expires)) {
        return null;
      }
      String decodedHash = new String(Base64.getDecoder().decode(encodedHash), StandardCharsets.UTF_8);
      return new RefreshTokenCacheValue(decodedHash, LocalDateTime.parse(expires));
    } catch (Exception ex) {
      log.warn("[RefreshTokenCache] Deserialize 예외 - 형식 오류");
      return null;
    }
  }
}
