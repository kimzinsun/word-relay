# 빌드 스테이지: Spring Boot 서버 빌드
FROM arm64v8/eclipse-temurin:17-jdk AS build
# 작업 디렉토리 설정
WORKDIR /app
# 빌드에 필요한 Gradle 파일만 먼저 복사 (캐시 최적화)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
# 필요한 권한 부여
RUN chmod +x ./gradlew
# 의존성 다운로드 (캐시 레이어 활용)
RUN ./gradlew dependencies --no-daemon
# 소스 코드 복사
COPY src src
# 애플리케이션 빌드
RUN ./gradlew build -x test --no-daemon

# 실행 스테이지: 최종 이미지 생성
FROM arm64v8/eclipse-temurin:17-jre-ubi9-minimal
# 작업 디렉토리 설정
WORKDIR /app
# 빌드 스테이지에서 빌드된 JAR 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar
# 환경 변수 설정
ENV SPRING_PROFILES_ACTIVE=prod
# 애플리케이션 포트 노출
EXPOSE 8080
# 비 루트 사용자로 실행 (보안 강화)
RUN microdnf install -y shadow-utils && \
    groupadd -r spring && \
    useradd -r -g spring spring && \
    microdnf clean all
USER spring:spring
# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
