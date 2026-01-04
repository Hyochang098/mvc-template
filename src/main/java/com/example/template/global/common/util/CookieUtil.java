package com.example.template.global.common.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;

/**
 * 토큰 탈취에 대한 문제로 인하여 토큰 발급시 토큰을 쿠키에 담기 위해 작성
 * 생성시 재발급시 create, 로그아웃 시 clean, 백엔드 내에서 읽을 땐 getCookie
 */
@Component
public class CookieUtil {

  private static boolean secure;
  private static String sameSite;
  private static String domain;
  private static Duration accessTokenMaxAge;
  private static Duration refreshTokenMaxAge;

  public CookieUtil(
      @Value("${security.cookie.secure:true}") boolean secure,
      @Value("${security.cookie.same-site:Lax}") String sameSite,
      @Value("${security.cookie.domain:}") String domain,
      @Value("${security.cookie.access-max-age-seconds:1800}") long accessTokenMaxAgeSeconds,
      @Value("${security.cookie.refresh-max-age-seconds:604800}") long refreshTokenMaxAgeSeconds
  ) {
    CookieUtil.secure = secure;
    CookieUtil.sameSite = sameSite;
    CookieUtil.domain = domain;
    CookieUtil.accessTokenMaxAge = Duration.ofSeconds(Math.max(accessTokenMaxAgeSeconds, 0));
    CookieUtil.refreshTokenMaxAge = Duration.ofSeconds(Math.max(refreshTokenMaxAgeSeconds, 0));
  }

  public static HttpHeaders createCookie(String accessToken, String refreshToken) {
    ResponseCookie.ResponseCookieBuilder refreshCookieBuilder = ResponseCookie.from("refreshToken", refreshToken)
        .httpOnly(true)
        .secure(secure)
        .path("/")
        .maxAge(refreshTokenMaxAge)
        .sameSite(sameSite);
    ResponseCookie.ResponseCookieBuilder accessCookieBuilder = ResponseCookie.from("accessToken", accessToken)
        .httpOnly(true)
        .secure(secure)
        .path("/")
        .maxAge(accessTokenMaxAge)
        .sameSite(sameSite);

    if (domain != null && !domain.isBlank()) {
      refreshCookieBuilder.domain(domain);
      accessCookieBuilder.domain(domain);
    }

    ResponseCookie refreshCookie = refreshCookieBuilder.build();
    ResponseCookie accessCookie = accessCookieBuilder.build();

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    headers.add(HttpHeaders.SET_COOKIE, accessCookie.toString());
    return headers;
  }

  public static HttpHeaders cleanCookies() {
    ResponseCookie.ResponseCookieBuilder refreshCookieBuilder = ResponseCookie.from("refreshToken", null)
        .httpOnly(true)
        .secure(secure)
        .path("/")
        .maxAge(0)
        .sameSite(sameSite);
    ResponseCookie.ResponseCookieBuilder accessCookieBuilder = ResponseCookie.from("accessToken", null)
        .httpOnly(true)
        .secure(secure)
        .path("/")
        .maxAge(0)
        .sameSite(sameSite);

    if (domain != null && !domain.isBlank()) {
      refreshCookieBuilder.domain(domain);
      accessCookieBuilder.domain(domain);
    }

    ResponseCookie refreshCookie = refreshCookieBuilder.build();
    ResponseCookie accessCookie = accessCookieBuilder.build();

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    headers.add(HttpHeaders.SET_COOKIE, accessCookie.toString());
    return headers;
  }

  public static String getCookieValue(HttpServletRequest request, String cookieName) {
    if (request.getCookies() == null) {
      return null;
    }

    return Arrays.stream(request.getCookies())
        .filter(cookie -> cookie.getName().equals(cookieName))
        .map(Cookie::getValue)
        .findFirst()
        .orElse(null);
  }

}