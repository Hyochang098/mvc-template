# MVC Template 가이드

팀원 모두가 동일한 기준으로 작업할 수 있도록 패키지 역할과 작업 위치를 정리했습니다. 신규 기능을 추가하실 때 아래 원칙을 참고해 주세요.

## 전체 구조
```
src/main/java/com/example/template
├─ MvcTemplateApplication.java      # 부트스트랩
├─ global/                          # 전역 레이어 (공통/설정/보안)
│  ├─ common/                       # 전역 공통(도메인 무관)
│  │  ├─ exception/                 # ApiException, ErrorMessage, GlobalExceptionHandler
│  │  ├─ util/                      # ApiResult, CookieUtil 등 순수 유틸
│  │  └─ entity/                    # BaseEntity, Role 등 공용 엔티티/값
│  ├─ config/                       # SecurityConfig, SwaggerConfig 등 전역 설정
│  └─ security/                     # 인증/인가 필터, 토큰, UserPrincipal
└─ domain/                          # 도메인별 하위 패키지
   ├─ user/                         # 사용자 도메인
   │  ├─ controller/                # REST 컨트롤러
   │  ├─ dto/                       # 요청/응답 DTO
   │  ├─ entity/                    # JPA 엔티티
   │  ├─ repository/                # Spring Data JPA 리포지토리
   │  └─ service/                   # 서비스 인터페이스/구현
   └─ refreshtoken/                 # 리프레시 토큰 도메인
      ├─ entity/
      └─ repository/
```

## 패키지별 역할과 작성 가이드
### global/common
- `exception`: `ApiException`, `ErrorMessage`, `GlobalExceptionHandler`로 예외 처리를 일관되게 작성했습니다.
- `util`: `ApiResult`(공통 응답 포맷), `CookieUtil` 등 상태 없는 순수 유틸을 모았습니다.
- `entity`: `BaseEntity`, `Role` 등 전역에서 재사용하는 엔티티/값 객체를 두었습니다.
### global/config
- 보안/Swagger 등 전역 설정을 배치했습니다. 외부 연동 설정(S3, Redis, MQ 등)도 여기에서 관리합니다.
### global/security
- JWT 발급/검증(`JwtTokenProvider`), 인증 필터(`JwtAuthenticationFilter`), 인증/인가 실패 핸들러, `CustomUserDetailsService`, `UserPrincipal`을 모았습니다.
### domain
- `controller`: REST 엔드포인트를 두었으며 입력 검증(`@Valid`)과 Swagger 문서화를 함께 작성했습니다.
- `dto`: 요청/응답 전용 DTO를 두었고 `@Schema`로 스펙/예제를 명시했습니다.
- `service`: 인터페이스/구현을 분리했고 트랜잭션·비즈니스 규칙·예외 발생을 책임지도록 구성했습니다.
- `repository`: JPA 리포지토리를 두었고 쿼리 로직만 담당하게 했습니다.
- `entity`: JPA 엔티티와 도메인 행위를 정의했습니다.

## 개발 원칙
- 요청/응답 DTO에는 `@Schema`와 예제 값을 넣어 Swagger 문서화를 일관되게 유지했습니다.
- 컨트롤러의 `@ApiResponse.description`은 사용자에게 바로 노출 가능한 문구로 작성했습니다.
- 비즈니스 검증 실패나 도메인 오류는 서비스 계층에서 `ApiException.of(HttpStatus, ErrorMessage)`로 던지고, 메시지는 `ErrorMessage` 상수를 사용했습니다.
- 공통 응답은 `ApiResult`로 감싸 성공/실패 포맷을 통일했습니다.
- 시큐리티가 필요한 엔드포인트에는 `@AuthenticationPrincipal UserPrincipal`을 사용했고, 인증/인가 실패는 전역 핸들러에 위임했습니다.

## Swagger/문서화
- 새로운 엔드포인트를 추가할 때 `@Operation`에 summary/description을 명확히 작성했고, `@ApiResponses`로 성공/오류 케이스를 모두 기술했습니다.
- DTO에는 `@Schema(description, example, nullable)`를 채워 실제 스펙을 그대로 노출했습니다.

## 예외 처리
- 전역 예외는 `GlobalExceptionHandler`에서 처리하도록 두었고, 서비스/컨트롤러는 의미 있는 메시지와 상태코드만 전달했습니다.
- 입력값 검증은 `@Valid`, `@Validated`로 처리했고, 커스텀 검증 실패는 `ApiException`으로 변환했습니다.

## 테스트/리소스
- 공통 설정은 `application.yml`에서 관리하도록 두었고 활성 프로필은 환경 변수(`SPRING_PROFILES_ACTIVE`)로만 지정했습니다.
- 프로필 분리:
  - `application-local.yml`: H2(create-drop), 로컬 CORS(origin 명시), 로컬 디폴트 JWT 시크릿을 사용했습니다.
  - `application-test.yml`: 테스트용 H2(create-drop), 최소 설정, 테스트 전용 JWT 시크릿을 사용했습니다.
  - `application-deploy.yml`: 배포/스테이징/개발 서버 공용으로 사용하며 DB/CORS/JWT 시크릿은 ENV로만 설정했습니다. `JPA_DDL_AUTO` 등은 환경 변수로 조정하도록 두었습니다.

팀 내에서 새로운 도메인을 추가할 때는 `domain/<new>/` 하위에 동일한 계층(controller/dto/entity/repository/service)을 생성해 일관된 구조를 유지하세요.

## 공유 전 점검
- `.env` 파일은 추적 대상에서 제외했습니다. 팀원에게 공유할 때는 `example.env`를 복사해 개별 `.env`를 만들고, 실제 비밀 값은 각자 환경 변수로만 설정하세요.
- 로컬/테스트용 기본 JWT 시크릿은 예제 값일 뿐이므로 배포 환경에서는 반드시 새로운 시크릿을 지정해야 합니다.
