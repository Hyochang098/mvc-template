package com.example.template.global.config;

import com.example.template.domain.refreshtoken.service.RefreshTokenCacheValue;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

  @Value("${cache.refresh-token.ttl-seconds:900}")
  private long refreshTokenTtlSeconds;

  @Value("${cache.access-token-blacklist.ttl-seconds:1800}")
  private long accessTokenBlacklistTtlSeconds;

  @Bean
  public Cache<Long, RefreshTokenCacheValue> refreshTokenCache() {
    return Caffeine.newBuilder()
        .expireAfterWrite(refreshTokenTtlSeconds, TimeUnit.SECONDS)
        .maximumSize(10_000)
        .build();
  }

  @Bean
  public Cache<String, Boolean> accessTokenBlacklistCache() {
    return Caffeine.newBuilder()
        .expireAfterWrite(accessTokenBlacklistTtlSeconds, TimeUnit.SECONDS)
        .maximumSize(50_000)
        .build();
  }
}
