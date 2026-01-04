# 테스트 작성 가이드 (JUnit5 / Mockito / given-when-then)

## 기본 원칙
- 테스트명은 시나리오를 드러내는 영어 문장으로 작성 (한글 DisplayName 병행 가능).
- `given-when-then` 패턴으로 준비/행동/검증을 명확히 분리.
- 외부 의존성(DB, 인증 등)은 Mockito로 목킹하고, 비즈니스 규칙만 검증.
- 입력/상태/부작용을 한 테스트에 하나씩만 검증해 실패 원인을 빠르게 찾는다.

## 패턴 예시
```java
@Test
@DisplayName("signUp - 신규 이메일이면 암호화 후 저장한다")
void signUp_saves_whenEmailIsNew() {
  // given: 선행 조건/목킹
  SignUpRequestDto req = new SignUpRequestDto("new@test.com", "Password123!", "홍길동");
  given(userRepository.existsByEmail("new@test.com")).willReturn(false);
  given(passwordEncoder.encode("Password123!")).willReturn("encodedPw");
  given(userRepository.save(any(User.class))).willReturn(
      User.builder().userId(1L).email("new@test.com").password("encodedPw").name("홍길동").role(Role.GENERAL).build()
  );

  // when: 대상 메서드 실행
  authService.signUp(req);

  // then: 결과/부작용 검증
  verify(userRepository).save(argThat(saved ->
      saved.getEmail().equals("new@test.com")
          && saved.getPassword().equals("encodedPw")
          && saved.getRole() == Role.GENERAL
  ));
}
```

## 작성 요령
- **given**: 입력값, 목킹 응답, 필요하면 time/UUID 등 결정적 값 주입.
- **when**: 테스트 대상 메서드 한 번만 호출.
- **then**: 상태/반환값/assert + 부작용(`verify`) 분리. 예외 검증 시 `assertThatThrownBy` 사용.
- 인증/보안 흐름은 `AuthenticationManager`, `JwtTokenProvider` 등 외부 의존성을 모두 목킹하고, 생성/저장 호출 여부만 본다.
- DTO 매핑 검증은 필수 필드만

## 자주 쓰는 어서션/목킹 스니펫
- 예외 메시지 확인: `assertThatThrownBy(() -> ...).hasMessageContaining("...")`
- 상태코드 확인: `assertThat(((ApiException) ex).getCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());`
- 인자 검증: `verify(repo).save(argThat(entity -> ...))`
- 호출 안 되었는지: `verify(repo, never()).save(any())`

## 실행 방법
- CLI: `./gradlew test -Dspring.profiles.active=test`
- IDE: Run/Debug 환경 변수 `SPRING_PROFILES_ACTIVE=test` 설정 후 실행

