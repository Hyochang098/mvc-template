package com.example.template.domain.user.service;


import com.example.template.domain.user.dto.LoginRequestDto;
import com.example.template.domain.user.dto.SignUpRequestDto;
import com.example.template.domain.user.dto.TokenResponseDto;

public interface AuthService {

  /**
   * 회원가입
   */
  void signUp(SignUpRequestDto signUpRequestDto);

  /**
   * 로그인
   */
  TokenResponseDto login(LoginRequestDto loginRequest);

  /**
   * 토큰 재발급
   */
  TokenResponseDto refreshToken(String refreshToken);

  /**
   * 로그아웃 - Refresh Token DB에서 삭제
   */
  void logout(Long userId);

  /**
   * 이메일 사용 가능 여부 확인
   */
  boolean isEmailAvailable(String email);
}
