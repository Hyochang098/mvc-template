# Docker 배포 가이드 (MySQL)

## 구성 개요
- `Dockerfile`: 멀티스테이지로 JAR 빌드 후 경량 JDK로 실행
- `docker-compose.yml`: 앱 컨테이너 + MySQL 8.4
- `ex_dev.env`: compose에서 불러올 환경 변수 예시
- 기본값을 넣지 않으므로 실제 실행 전 `.env` 또는 환경 변수를 반드시 채워야 함

## 사전 준비
1. `ex_dev.env`를 열어 값 확인 후 필요한 항목을 교체  
   - `JWT_SECRET`는 32자 이상 임의 문자열로 교체  
   - DB 포트는 기본적으로 외부에 노출하지 않음(필요 시 직접 추가)
2. 로컬에 Docker와 Docker Compose가 설치되어 있어야 함

## 실행 절차
```bash
# 1) 환경 변수 파일 준비
cp ex_dev.env .env    # 필요 시 ex_dev.env 직접 수정 후 사용해도 무방

# 2) 빌드 및 기동
docker compose build
docker compose up -d

# 3) 로그 확인
docker compose logs -f app
```

## 주요 환경 변수
- DB: `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_ROOT_PASSWORD`
- JPA: `JPA_DDL_AUTO`(기본 none), `JPA_SHOW_SQL`, `JPA_FORMAT_SQL`
- JWT: `JWT_SECRET`, `JWT_ACCESS_TTL`, `JWT_REFRESH_TTL`
- CORS: `CORS_ALLOWED_ORIGINS`
- 쿠키: `COOKIE_ACCESS_MAX_AGE_SECONDS`, `COOKIE_REFRESH_MAX_AGE_SECONDS`, `COOKIE_DOMAIN`

## 헬스체크 및 대기
- MySQL 컨테이너가 `healthy` 상태가 되면 앱 컨테이너가 기동됨
- 필요 시 `docker ps`로 상태를 확인하거나 `docker compose logs db`로 MySQL 로그를 확인

## 자주 발생하는 문제
- 포트 충돌: 8080/3306이 이미 사용 중이면 `docker-compose.yml`의 `ports` 매핑을 변경
- JWT 미설정: `JWT_SECRET`가 비어 있으면 앱 기동 시 실패
- DB 연결 실패: `DB_URL` 기본값은 `db` 서비스 이름을 사용하므로 compose 네트워크 내에서만 통신. 외부 DB를 쓰려면 `DB_URL`을 외부 호스트로 교체

