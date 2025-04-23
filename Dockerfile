# 1단계: 빌드 단계
FROM gradle:8.5-jdk21-alpine AS build

WORKDIR /app

COPY . .

# 2단계: 실행 단계
FROM amazoncorretto:21-alpine3.21-jdk

WORKDIR /app

# build/libs 폴더의 JAR 복사
COPY --from=build /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]