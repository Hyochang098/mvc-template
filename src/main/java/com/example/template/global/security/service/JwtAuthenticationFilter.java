package com.example.template.global.security.service;

import com.example.template.domain.user.entity.User;
import com.example.template.domain.user.repository.UserRepository;
import com.example.template.global.common.entity.Role;
import com.example.template.global.common.util.CookieUtil;
import com.example.template.global.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;
  private final UserRepository userRepository;

  @Value("${security.jwt.check-db:false}")
  private boolean checkUserStateWithDb;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String accessToken = getAccessTokenFromRequest(request);

    // Access Token이 유효한 경우만 인증 처리
    if (StringUtils.hasText(accessToken) && jwtTokenProvider.validateToken(accessToken)) {
      authenticateWithToken(request, accessToken);
    }
    // refresh Token 기반 재발급 로직은 없음 필요시 추가 예정
    filterChain.doFilter(request, response);
  }

  private void authenticateWithToken(HttpServletRequest request, String token) {
    try {
      // 1. 토큰에서 유저ID(useerId), 이메일(subject)과 role(claim) 추출
      Long userId =  jwtTokenProvider.getUserIdFromToken(token);
      String email = jwtTokenProvider.getEmailFromToken(token);
      String role = jwtTokenProvider.getRoleFromToken(token);

      if (checkUserStateWithDb) {
        UserPrincipal userPrincipal = userRepository.findById(userId)
            .filter(user -> email.equalsIgnoreCase(user.getEmail()))
            .map(this::buildPrincipalFromUser)
            .orElse(null);

        if (userPrincipal == null) {
          log.warn("[JWT Filter] 인증 실패 - DB 상태와 토큰 정보 불일치 또는 사용자 없음, userId={}", userId);
          SecurityContextHolder.clearContext();
          return;
        }

        setAuthentication(request, userPrincipal);
        log.debug("[JWT Filter] 인증 성공 (DB 검증 포함)");
        return;
      }

      UserPrincipal userPrincipal = new UserPrincipal(
          userId,
          email,
          Role.valueOf(role),
          null
      );

      setAuthentication(request, userPrincipal);
      log.debug("[JWT Filter] 인증 성공 (토큰 기반)");

    } catch (Exception e) {
      // 토큰 파싱 실패, role 변환 실패 등 예외 발생 시
      log.warn("[JWT Filter] 인증 실패: {}", e.getMessage());
      SecurityContextHolder.clearContext();
    }
  }

  private void setAuthentication(HttpServletRequest request, UserPrincipal userPrincipal) {
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(
            userPrincipal,
            null,
            userPrincipal.getAuthorities()
        );

    authentication.setDetails(
        new WebAuthenticationDetailsSource().buildDetails(request)
    );

    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  private UserPrincipal buildPrincipalFromUser(User user) {
    return new UserPrincipal(
        user.getUserId(),
        user.getEmail(),
        user.getRole(),
        null
    );
  }


  private String getAccessTokenFromRequest(HttpServletRequest request) {
    // 1. Authorization 헤더에서 Bearer 토큰 확인
    String bearerToken = request.getHeader("Authorization");
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    // 2. 쿠키에서 accessToken 확인
    String cookieToken = CookieUtil.getCookieValue(request, "accessToken");
    if (StringUtils.hasText(cookieToken)) {
      return cookieToken;
    }

    return null;
  }

}

