# 1단계: 빌드 단계
FROM gradle:8.5-jdk21-alpine AS build

WORKDIR /app

COPY . .

# Python 설치
RUN apt-get update && \
    apt-get install -y python3 python3-pip && \
    pip3 install --upgrade pip

# requirements.txt 복사 및 설치
COPY requirements.txt .
RUN pip3 install -r requirements.txt

# 2단계: 실행 단계
FROM amazoncorretto:21-alpine3.21-jdk

WORKDIR /app

# build/libs 폴더의 JAR 복사
COPY --from=build /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]