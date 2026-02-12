# 레포 루트가 백엔드일 때 사용 (napolmebackend 레포용)
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x ./gradlew
COPY src ./src
RUN ./gradlew build -x test

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
