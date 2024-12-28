FROM amazoncorretto:21

# JAR 파일 경로를 동적으로 설정
ARG JAR_FILE=build/libs/user-service-0.0.1-SNAPSHOT.jar

# 애플리케이션 JAR 복사
COPY ${JAR_FILE} app.jar

# 컨테이너 시작 시 JAR 실행
ENTRYPOINT ["java", "-jar", "/app.jar"]