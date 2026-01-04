package com.example.template.domain.user.controller;

import com.example.template.domain.user.dto.UserResponseDto;
import com.example.template.domain.user.service.UserService;
import com.example.template.global.common.exception.ErrorMessage;
import com.example.template.global.common.util.ApiResult;
import com.example.template.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "회원", description = "회원 관련 API")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "본인 정보 조회", description = "로그인 한 유저의 정보를 반환합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "본인 정보 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDto.class))),
        @ApiResponse(responseCode = "401", description = "인증이 필요합니다.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResult.class))),
        @ApiResponse(responseCode = "404", description = "해당 유저를 찾을 수 없습니다.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResult.class))),
        @ApiResponse(responseCode = "500", description = "알 수 없는 서버 오류가 발생했습니다.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResult.class)))
    })
    public ResponseEntity<ApiResult<UserResponseDto>> findme(@AuthenticationPrincipal UserPrincipal userPrincipal){
        return ResponseEntity.ok(ApiResult.success(userService.findMe(userPrincipal.getUserId())));
    }
}
