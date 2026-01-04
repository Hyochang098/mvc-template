package com.example.template.domain.user.service.impl;


import com.example.template.domain.refreshtoken.entity.RefreshToken;
import com.example.template.domain.refreshtoken.repository.RefreshTokenRepository;
import com.example.template.domain.user.dto.LoginRequestDto;
import com.example.template.domain.user.dto.SignUpRequestDto;
import com.example.template.domain.user.dto.TokenResponseDto;
import com.example.template.domain.user.entity.User;
import com.example.template.domain.user.repository.UserRepository;
import com.example.template.domain.user.service.AuthService;
import com.example.template.global.common.entity.Role;
import com.example.template.global.common.exception.ApiException;
import com.example.template.global.common.exception.ErrorMessage;
import com.example.template.global.security.service.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

  private final UserRepository userRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final AuthenticationManager authenticationManager;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public void signUp(SignUpRequestDto signUpRequestDto) {
    log.info("[AuthService] 회원가입 시도");

    String normalizedEmail = normalizeEmail(signUpRequestDto.email());
    if (userRepository.existsByEmail(normalizedEmail)) {
      log.warn("[AuthService] 회원가입 실패 - 이메일 중복");
      throw ApiException.of(HttpStatus.CONFLICT, ErrorMessage.EMAIL_ALREADY_EXISTS);
    }

    User user = SignUpRequestDto.of(signUpRequestDto, normalizedEmail);

    user.changePassword(passwordEncoder.encode(signUpRequestDto.password()));
    user.changeRole(Role.GENERAL);

    User saved = userRepository.save(user);
    log.info("[AuthService] 회원가입 완료, userId={}", saved.getUserId());
  }

  @Override
  @Transactional
  public TokenResponseDto login(LoginRequestDto loginRequest) {
    log.info("[AuthService] 로그인 시도");
    try {
      String normalizedEmail = normalizeEmail(loginRequest.email());
      authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(normalizedEmail, loginRequest.password())
      );

      User user = userRepository.findByEmail(normalizedEmail)
          .orElseThrow(() -> {
            log.warn("[AuthService] 로그인 실패 - 사용자 정보 없음");
            return ApiException.of(HttpStatus.NOT_FOUND, ErrorMessage.USER_NOT_FOUND);
          });

      String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getEmail(),
          user.getRole().name());

      String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId(), user.getEmail(),
          user.getRole().name());

      String refreshTokenHash = passwordEncoder.encode(refreshToken);
      LocalDateTime refreshExpiresAt = calculateRefreshTokenExpiry();

      refreshTokenRepository.findByUserId(user.getUserId())
          .ifPresentOrElse(
              existingToken -> existingToken.updateToken(refreshTokenHash, refreshExpiresAt),
              () -> refreshTokenRepository.save(
                  RefreshToken.builder()
                      .userId(user.getUserId())
                      .tokenHash(refreshTokenHash)
                      .expiresAt(refreshExpiresAt)
                      .build()
              )
          );

      log.info("[AuthService] 로그인 성공 userId={}", user.getUserId());

      return new TokenResponseDto(
          user.getUserId(),
          user.getEmail(),
          user.getName(),
          user.getRole().name(),
          accessToken,
          refreshToken
      );
    } catch (AuthenticationException ex) {
      log.warn("[AuthService] 로그인 실패 - 인증 실패: {}", ex.getMessage());
      throw ApiException.of(HttpStatus.BAD_REQUEST, "이메일 또는 비밀번호가 올바르지 않습니다.");
    }
  }

  @Override
  @Transactional
  public TokenResponseDto refreshToken(String refreshToken) {
    log.info("[AuthService] 토큰 재발급 시도");

    if (!jwtTokenProvider.validateToken(refreshToken)) {
      log.warn("[AuthService] 토큰 재발급 실패 - reason={}",ErrorMessage.INVALID_REFRESH_TOKEN);
      throw ApiException.of(HttpStatus.UNAUTHORIZED, ErrorMessage.INVALID_REFRESH_TOKEN);
    }

    Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

    RefreshToken storedToken = refreshTokenRepository.findByUserId(userId)
        .orElseThrow(() -> {
          log.error("[AuthService] 토큰 재발급 실패 - 저장된 리프레시 토큰 없음, userId={}", userId);
          return ApiException.of(HttpStatus.NOT_FOUND, ErrorMessage.REFRESH_TOKEN_NOT_FOUND);
        });

    if (storedToken.isExpired(LocalDateTime.now())) {
      log.warn("[AuthService] 토큰 재발급 실패 - 저장된 토큰 만료, userId={}", userId);
      throw ApiException.of(HttpStatus.UNAUTHORIZED, ErrorMessage.INVALID_REFRESH_TOKEN);
    }

    boolean refreshTokenMatches = passwordEncoder.matches(refreshToken, storedToken.getTokenHash())
        || refreshToken.equals(storedToken.getTokenHash()); // 이전 평문 저장분 호환
    if (!refreshTokenMatches) {
      log.warn("[AuthService] 토큰 재발급 실패 - 토큰 불일치, userId={}", userId);
      throw ApiException.of(HttpStatus.UNAUTHORIZED, ErrorMessage.INVALID_REFRESH_TOKEN);
    }

    String email = normalizeEmail(jwtTokenProvider.getEmailFromToken(refreshToken));
    String role = jwtTokenProvider.getRoleFromToken(refreshToken);
    
    String newAccessToken = jwtTokenProvider.createAccessToken(userId, email, role);
    String newRefreshToken = jwtTokenProvider.createRefreshToken(userId, email, role);

    String newRefreshTokenHash = passwordEncoder.encode(newRefreshToken);
    LocalDateTime newRefreshExpiresAt = calculateRefreshTokenExpiry();

    storedToken.updateToken(newRefreshTokenHash, newRefreshExpiresAt);
    refreshTokenRepository.save(storedToken);

    log.info("[AuthService] 토큰 재발급 성공, userId={}", userId);

    return new TokenResponseDto(
        userId,
        email,
        null, // 이름은 필요시에만 조회
        role,
        newAccessToken,
        newRefreshToken
    );
  }


  @Override
  @Transactional
  public void logout(Long userId) {
    log.info("[AuthService] 로그아웃 시도 userId={}", userId);
    refreshTokenRepository.deleteByUserId(userId);
    log.info("[AuthService] 로그아웃 완료 userId={}", userId);
  }


  @Override
  @Transactional(readOnly = true)
  public boolean isEmailAvailable(String email) {
    String normalizedEmail = normalizeEmail(email);
    return !userRepository.existsByEmail(normalizedEmail);
  }

  private String normalizeEmail(String email) {
    if (email == null || email.trim().isEmpty()) {
      log.warn("[AuthService] 이메일 정규화 실패 - 이메일 누락");
      throw ApiException.of(HttpStatus.BAD_REQUEST, "이메일은 필수 입력 값입니다.");
    }
    return email.trim().toLowerCase();
  }

  private LocalDateTime calculateRefreshTokenExpiry() {
    long seconds = jwtTokenProvider.getRefreshTokenValidityInSeconds();
    if (seconds <= 0) {
      return LocalDateTime.now();
    }
    return LocalDateTime.now().plusSeconds(seconds);
  }
}
