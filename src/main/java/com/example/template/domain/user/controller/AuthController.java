package com.example.template.domain.user.controller;


import com.example.template.domain.user.dto.LoginRequestDto;
import com.example.template.domain.user.dto.SignUpRequestDto;
import com.example.template.domain.user.dto.TokenResponseDto;
import com.example.template.domain.user.service.AuthService;
import com.example.template.global.common.exception.ErrorMessage;
import com.example.template.global.common.util.ApiResult;
import com.example.template.global.common.util.CookieUtil;
import com.example.template.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@Tag(name = "인증", description = "인증 관련 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

  private final AuthService authService;

  @Operation(summary = "회원 가입", description = "새로운 사용자를 등록합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "회원 가입 완료", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResult.class))),
      @ApiResponse(responseCode = "400", description = "요청 값 검증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResult.class))),
      @ApiResponse(responseCode = "409", description = "존재하는 이메일입니다.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResult.class))),
      @ApiResponse(responseCode = "500", description = "알 수 없는 서버 오류가 발생했습니다.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResult.class)))
  })
  @PostMapping("/signUp")
  public ResponseEntity<ApiResult<Void>> signUp(
      @Valid @RequestBody SignUpRequestDto signUpRequestDto) {
    authService.signUp(signUpRequestDto);

    return ResponseEntity.ok()
        .body(ApiResult.success(null));
  }

  @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하고 토큰을 응답 바디로 반환합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "로그인 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TokenResponseDto.class))),
      @ApiResponse(responseCode = "400", description = "요청 값 검증 실패 또는 이메일/비밀번호 불일치", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResult.class))),
      @ApiResponse(responseCode = "404", description = "해당 유저를 찾을 수 없습니다.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResult.class))),
      @ApiResponse(responseCode = "500", description = "알 수 없는 서버 오류가 발생했습니다.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResult.class)))
  })
  @PostMapping("/login")
  public ResponseEntity<ApiResult<TokenResponseDto>> login(
      @Valid @RequestBody LoginRequestDto loginRequest) {

    TokenResponseDto tokenResponse = authService.login(loginRequest);

    return ResponseEntity.ok(ApiResult.success(tokenResponse));
  }

  @Operation(summary = "토큰 재발급", description = "리프레시 토큰으로 새로운 액세스/리프레시 토큰을 응답 바디로 반환합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "토큰 재발급 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TokenResponseDto.class))),
      @ApiResponse(responseCode = "400", description = "리프레시 토큰 누락 또는 검증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResult.class))),
      @ApiResponse(responseCode = "401", description = "유효하지 않은 리프레시 토큰입니다.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResult.class))),
      @ApiResponse(responseCode = "404", description = "해당 리프래시 토큰을 찾을 수 없습니다.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResult.class))),
      @ApiResponse(responseCode = "500", description = "알 수 없는 서버 오류가 발생했습니다.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResult.class)))
  })
  @PostMapping("/refresh")
  public ResponseEntity<ApiResult<TokenResponseDto>> refresh(
      HttpServletRequest request,
      @RequestHeader(value = "refreshToken", required = false) String refreshTokenHeader) {

    String refreshToken = refreshTokenHeader;
    if (refreshToken == null || refreshToken.isBlank()) {
      refreshToken = CookieUtil.getCookieValue(request, "refreshToken");
    }

    if (refreshToken == null || refreshToken.isBlank()) {
      return ResponseEntity.badRequest()
          .body(ApiResult.fail(400, "리프레시 토큰이 없습니다."));
    }

    TokenResponseDto tokenResponse = authService.refreshToken(refreshToken);

    return ResponseEntity.ok(ApiResult.success(tokenResponse));
  }

  @Operation(summary = "로그아웃", description = "로그아웃하고 쿠키를 삭제하며 DB의 refresh token을 제거합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "로그아웃 성공"),
      @ApiResponse(responseCode = "401", description = "인증이 필요합니다.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResult.class))),
      @ApiResponse(responseCode = "500", description = "알 수 없는 서버 오류가 발생했습니다.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResult.class)))
  })
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      HttpServletRequest request,
      @AuthenticationPrincipal UserPrincipal userPrincipal) {

    String accessToken = CookieUtil.getCookieValue(request, "accessToken");
    if (accessToken == null || accessToken.isBlank()) {
      String bearerToken = request.getHeader("Authorization");
      if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
        accessToken = bearerToken.substring(7);
      }
    }

    authService.logout(userPrincipal.getUserId(), accessToken);

    return ResponseEntity.noContent()
        .headers(CookieUtil.cleanCookies())
        .build();
  }

  @Operation(summary = "이메일 사용 가능 여부 확인", description = "회원가입 전 이메일 중복 여부를 확인합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "사용 가능 여부 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResult.class))),
      @ApiResponse(responseCode = "400", description = "요청 값 검증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResult.class))),
      @ApiResponse(responseCode = "500", description = ErrorMessage.INTERNAL_SERVER_ERROR, content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResult.class)))
  })
  @GetMapping("/check-email")
  public ResponseEntity<ApiResult<Boolean>> checkEmailAvailable(
      @RequestParam
      @NotBlank(message = "이메일은 필수 값입니다.")
      @Email(message = "올바른 이메일 형식이 아닙니다.") String email) {

    String normalizedEmail = email.trim();

    boolean isAvailable = authService.isEmailAvailable(normalizedEmail);
    return ResponseEntity.ok(ApiResult.success(isAvailable));
  }
}
