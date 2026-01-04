package com.example.template.domain.user.dto;


import com.example.template.domain.user.entity.User;
import com.example.template.global.common.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 조회 응답 DTO")
public record UserResponseDto(
    @Schema(description = "사용자 ID", example = "1")
    Long userId,

    @Schema(description = "이메일", example = "user@example.com")
    String email,

    @Schema(description = "이름", example = "홍길동")
    String name,

    @Schema(description = "권한", example = "GENERAL")
    Role role
) {

  public static UserResponseDto from(User user) {
    return new UserResponseDto(
        user.getUserId(),
        user.getEmail(),
        user.getName(),
        user.getRole()
    );
  }
}
