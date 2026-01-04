package com.example.template.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "토큰 응답 DTO")
public record TokenResponseDto(
    @Schema(description = "사용자 ID", example = "1")
    Long userId,

    @Schema(description = "이메일", example = "user@example.com")
    String email,

    @Schema(description = "사용자 이름", example = "홍길동", nullable = true)
    String name,

    @Schema(description = "권한", example = "GENERAL")
    String role,

    @Schema(description = "액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9.access-token-payload")
    String accessToken,

    @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJIUzI1NiJ9.refresh-token-payload")
    String refreshToken
) {

}
