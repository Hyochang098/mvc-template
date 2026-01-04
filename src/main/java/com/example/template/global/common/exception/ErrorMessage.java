package com.example.template.global.common.exception;

public class ErrorMessage {

  // 400 Bad Request 
  public static final String BAD_REQUEST = "요청 파라미터가 올바르지 않습니다.";

  // 401 Unauthorized 
  public static final String UNAUTHORIZED = "인증이 필요합니다.";

  // 403 Forbidden 
  public static final String FORBIDDEN = "권한이 없습니다.";
  public static final String ACCESS_DENIED = "접근 권한이 없습니다.";

  // Conflict
  public static final String EMAIL_ALREADY_EXISTS="존재하는 이메일입니다.";

  // 500 Internal Server Error 
  public static final String INTERNAL_SERVER_ERROR = "알 수 없는 서버 오류가 발생했습니다.";

  // 도메인별 NOT_FOUND 에러 메시지 (404)  
  public static final String USER_NOT_FOUND = "해당 유저를 찾을 수 없습니다.";
  public static final String REFRESH_TOKEN_NOT_FOUND = "해당 리프래시 토큰을 찾을 수 없습니다.";

  public static final String INVALID_TOKEN = "유효하지 않은 에세스 토큰입니다. 다시 로그인해주세요.";
  public static final String MISSING_TOKEN = "인증 토큰이 필요합니다. 로그인해주세요.";

  public static final String INVALID_REFRESH_TOKEN="유효하지 않은 리프래시 토큰입니다.";
}
