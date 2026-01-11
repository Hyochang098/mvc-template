package com.example.template.global.security.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

  private final SecretKey secretKey;
  private final long accessTokenValidityInMilliseconds;
  private final long refreshTokenValidityInMilliseconds;

  public JwtTokenProvider(
      @Value("${jwt.secret:}") String secret,
      @Value("${jwt.access-token-validity-in-seconds:1800}") long accessTokenValidityInSeconds,
      @Value("${jwt.refresh-token-validity-in-seconds:604800}") long refreshTokenValidityInSeconds) {
    if (secret == null || secret.length() < 32) {
      throw new IllegalStateException("[JWT] secret 키가 설정되지 않았거나 32자 미만입니다. 환경 변수를 확인하세요.");
    }

    this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
    this.accessTokenValidityInMilliseconds = accessTokenValidityInSeconds * 1000;
    this.refreshTokenValidityInMilliseconds = refreshTokenValidityInSeconds * 1000;
  }

  /**
   * Access Token 생성
   */
  public String createAccessToken(Long userId, String email, String role) {
    return createToken(userId, email, role, accessTokenValidityInMilliseconds);
  }

  /**
   * Refresh Token 생성
   */
  public String createRefreshToken(Long userId, String email, String role) {
    return createToken(userId, email, role, refreshTokenValidityInMilliseconds);
  }

  /**
   * JWT 토큰 생성
   */
  private String createToken(Long userId, String email, String role, long validityInMilliseconds) {
    Date now = new Date();
    Date validity = new Date(now.getTime() + validityInMilliseconds);

    return Jwts.builder()
        .subject(email)
        .claim("userId", userId)
        .claim("role", role)
        .issuedAt(now)
        .expiration(validity)
        .signWith(secretKey)               // signWith(key, algorithm) 대신 signWith(key) 사용
        .compact();
  }

  /**
   * JWT 토큰에서 유저ID 추출
   */
  public Long getUserIdFromToken(String token) {
    return getClaimsFromToken(token).get("userId", Long.class);
  }

  /**
   * JWT 토큰에서 이메일 추출
   */
  public String getEmailFromToken(String token) {
    return getClaimsFromToken(token).getSubject();
  }

  /**
   * JWT 토큰에서 권한 추출
   */
  public String getRoleFromToken(String token) {
    return getClaimsFromToken(token).get("role", String.class);
  }

  /**
   * JWT 토큰에서 Claims 추출 (JJWT 0.12.x 권장 방식)
   */
  private Claims getClaimsFromToken(String token) {
    return Jwts.parser()                   // parserBuilder() → parser() 사용
        .verifyWith(secretKey)             // setSigningKey() → verifyWith() 사용
        .build()
        .parseSignedClaims(token)          // parseClaimsJws() → parseSignedClaims() 사용
        .getPayload();                     // getBody() → getPayload() 사용
  }

  /**
   * JWT 토큰 유효성 검증
   */
  public boolean validateToken(String token) {
    try {
      Claims claims = getClaimsFromToken(token);
      return !claims.getExpiration().before(new Date());
    } catch (ExpiredJwtException e) {
      log.warn("JWT 만료됨: {}", e.getMessage());
    } catch (UnsupportedJwtException e) {
      log.error("지원하지 않는 JWT: {}", e.getMessage());
    } catch (MalformedJwtException e) {
      log.error("잘못된 JWT 구조: {}", e.getMessage());
    } catch (SecurityException | SignatureException e) {
      log.error("JWT 서명 검증 실패: {}", e.getMessage());
    } catch (IllegalArgumentException e) {
      log.error("JWT 파라미터가 잘못됨: {}", e.getMessage());
    }
    return false;
  }

  /**
   * JWT 토큰 만료 확인
   */
  public boolean isTokenExpired(String token) {
    try {
      Date expiration = getClaimsFromToken(token).getExpiration();
      return expiration.before(new Date());
    } catch (ExpiredJwtException e) {
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public long getRefreshTokenValidityInSeconds() {
    return refreshTokenValidityInMilliseconds / 1000;
  }

  public long getAccessTokenValidityInSeconds() {
    return accessTokenValidityInMilliseconds / 1000;
  }

  public long getRemainingValidityInSeconds(String token) {
    try {
      Date expiration = getClaimsFromToken(token).getExpiration();
      long remainingMillis = expiration.getTime() - System.currentTimeMillis();
      return remainingMillis > 0 ? remainingMillis / 1000 : 0;
    } catch (ExpiredJwtException e) {
      return 0;
    } catch (Exception e) {
      log.debug("JWT 만료 시간 계산 실패");
      return 0;
    }
  }
}
