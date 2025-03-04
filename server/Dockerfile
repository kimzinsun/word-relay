# Word-Relay 프로젝트를 위한 Dockerfile

# 빌드 스테이지: Spring Boot 서버 빌드
FROM arm64v8/eclipse-temurin:17-alpine AS build

# 작업 디렉토리 설정
WORKDIR /app

# server 디렉토리의 내용을 복사
COPY . .

# 필요한 권한 부여
RUN chmod +x ./gradlew

# 애플리케이션 빌드
RUN ./gradlew build -x test --no-daemon

# 실행 스테이지: 최종 이미지 생성
FROM eclipse-temurin:17-jre-alpine

# 작업 디렉토리 설정
WORKDIR /app

# 빌드 스테이지에서 빌드된 JAR 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 환경 변수 설정
ENV SPRING_PROFILES_ACTIVE=prod

# 애플리케이션 포트 노출
EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
