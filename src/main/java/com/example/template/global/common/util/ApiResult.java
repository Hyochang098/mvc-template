package com.example.template.global.common.util;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "공통 API 응답 포맷")
public class ApiResult<T> {

  @Schema(description = "요청 성공 여부", example = "true")
  private boolean success;

  @Schema(description = "응답 메시지", example = "요청 성공")
  private String message;

  @Schema(description = "HTTP 상태 코드", example = "200")
  private int code;

  @Schema(description = "응답 데이터", nullable = true)
  private T data;

  public ApiResult(boolean success, String message, int code, T data) {
    this.success = success;
    this.message = message;
    this.code = code;
    this.data = data;
  }

  public static <T> ApiResult<T> success(T data) {
    return new ApiResult<T>(true, "요청 성공", 200, data);
  }

  public static <T> ApiResult<T> success(int code, String message, T data) {
    return new ApiResult<T>(true, message, code, data);
  }

  public static <T> ApiResult<T> fail(int code, String message) {
    return new ApiResult<T>(false, message, code, null);
  }
}