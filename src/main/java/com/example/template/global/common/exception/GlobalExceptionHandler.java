package com.example.template.global.common.exception;

import com.example.template.global.common.util.ApiResult;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * 요청 DTO(@RequestBody, @ModelAttribute 등) 유효성 검증 실패 시 발생.
   * <p>
   * - `@Valid`, `@Validated` 어노테이션이 붙은 DTO에서 필드 제약조건 위반이 발생하면 MethodArgumentNotValidException 또는
   * BindException 이 발생한다. - 예: `@NotBlank`, `@Size` 제약을 어겼을 때.
   * <p>
   * 처리 방식: - 첫 번째 필드 에러 메시지를 추출하여 클라이언트에 반환한다. - HTTP 상태코드 400(Bad Request) 응답을 내려준다.
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResult<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
    log.warn("[Validation Error] 요청 바디 검증 실패: {}", e.getMessage());
    return badRequestWithBindingResult(e.getBindingResult());
  }

  @ExceptionHandler(BindException.class)
  public ResponseEntity<ApiResult<Void>> handleBindException(BindException e) {
    log.warn("[Validation Error] 파라미터/모델 바인딩 검증 실패: {}", e.getMessage());
    return badRequestWithBindingResult(e.getBindingResult());
  }

  /**
   * Bean Validation 제약조건 위반 시 발생.
   * <p>
   * 주로 컨트롤러 메서드 파라미터 단위 검증에서 발생한다.
   * 예: `public void getUser(@Min(1) @PathVariable Long id)` 처럼,
   * 메서드 파라미터에 Validation 어노테이션을 붙였을 때 유효하지 않으면 ConstraintViolationException 이 발생한다.
   * <p>
   * 처리 방식: - 위반된 제약조건의 메시지를 추출하여 반환한다. - HTTP 상태코드 400(Bad Request) 응답을 내려준다.
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiResult<Void>> handleConstraintViolationException(
      ConstraintViolationException e) {
    log.warn("[Constraint Violation] 제약조건 위반: {}", e.getMessage());
    String errorMessage = e.getConstraintViolations().stream()
        .map(cv -> cv.getMessage())
        .findFirst()
        .orElse(ErrorMessage.BAD_REQUEST);
    return ResponseEntity.badRequest()
        .body(ApiResult.fail(HttpStatus.BAD_REQUEST.value(), errorMessage));
  }

  /**
   * 비즈니스 로직 검증 실패 시 발생.
   * <p>
   * - 서비스 단에서 잘못된 인자가 전달되었음을 의미할 때 IllegalArgumentException 을 던질 수 있다.
   * - 예: 회원가입 시 이미 존재하는 이메일로 요청이 들어온 경우.
   * <p>
   * 처리 방식:
   * - 예외 메시지를 그대로 클라이언트에 전달한다.
   * - HTTP 상태코드 400(Bad Request) 응답을 내려준다.
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResult<Void>> handleIllegalArgumentException(
      IllegalArgumentException e) {
    log.warn("[Business Logic Error] {}", e.getMessage());
    return ResponseEntity.badRequest()
        .body(ApiResult.fail(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
  }

  /**
   * 커스텀 API 예외 처리.
   * <p>
   * - 서비스/도메인 단에서 의도적으로 ApiException 을 던졌을 때 이 핸들러가 동작한다.
   * - 예: UserService 에서 사용자를 찾지 못했을 때 `throw ApiException.of(HttpStatus.NOT_FOUND, ErrorMessage.USER_NOT_FOUND)`
   * <p>
   * 처리 방식:
   * - ApiException 에 담긴 상태코드와 메시지를 그대로 응답에 반영한다.
   * - HTTP 상태코드는 예외 내부의 code 값을 따른다.
   *
   * 일관된 예외 처리와 다양한 상태코드 활용을 위해 IllegalArgumentException 보다는 ApiException.of(HttpStatus, ErrorMessage) 사용을 권장한다.
   */
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiResult<Void>> handleApiException(ApiException e) {
    log.error("API Exception: code={}, message={}", e.getCode(), e.getMessage());
    return ResponseEntity
        .status(e.getCode())
        .body(ApiResult.fail(e.getCode(), e.getMessage()));
  }

  /**
   * 그 외 모든 예외 처리
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResult<Void>> handleAllUncaughtException(Exception e) {
    log.error("Unhandled Exception occurred: {}", e.getMessage(), e);
    return ResponseEntity.internalServerError()
        .body(ApiResult.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(),
            ErrorMessage.INTERNAL_SERVER_ERROR));
  }

  private ResponseEntity<ApiResult<Void>> badRequestWithBindingResult(BindingResult bindingResult) {
    String errorMessage = bindingResult.getFieldErrors().stream()
        .map(FieldError::getDefaultMessage)
        .findFirst()
        .orElse(ErrorMessage.BAD_REQUEST);
    return ResponseEntity.badRequest()
        .body(ApiResult.fail(HttpStatus.BAD_REQUEST.value(), errorMessage));
  }
}
