# 1단계: 빌드 단계
FROM gradle:8.5-jdk21-jammy as build

WORKDIR /app

COPY . .

# Python 설치
RUN apt-get update && \
    apt-get install -y python3 python3-pip && \
    pip3 install --no-cache-dir \
        flask==2.0.1 \
        flask-cors==3.0.10 \
        PyMuPDF==1.21.1 \
        python-dotenv==0.19.0 \
        werkzeug==2.0.3

RUN gradle bootJar -x test --no-daemon


# 2단계: 실행 단계
FROM amazoncorretto:21-alpine3.21-jdk

WORKDIR /app

# build/libs 폴더의 JAR 복사
COPY --from=build /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]