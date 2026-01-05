FROM gradle:8.7-jdk17 AS build
WORKDIR /workspace
# 그레이들 캐시를 최대한 활용하기 위해 설정 파일을 먼저 복사
COPY build.gradle settings.gradle ./
COPY gradle gradle
# 소스 복사
COPY src src
# 테스트는 CI에서 수행한다고 가정하고 jar만 생성
RUN gradle clean bootJar -x test

FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
# 애플리케이션 표준 포트
EXPOSE 8080
# 타임존을 서울로 맞춤
RUN ln -snf /usr/share/zoneinfo/Asia/Seoul /etc/localtime && echo "Asia/Seoul" > /etc/timezone
# 빌드 결과물을 복사
COPY --from=build /workspace/build/libs/*.jar app.jar
# 컨테이너 실행 시 애플리케이션 기동
ENTRYPOINT ["java","-jar","/app/app.jar"]

