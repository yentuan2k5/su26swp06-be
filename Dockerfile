# Build Spring Boot bằng Maven và Java 21
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -B clean package -DskipTests


# Chạy ứng dụng bằng Java 21
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build \
    /app/target/scientific-journal-tracker-0.0.1-SNAPSHOT.jar \
    app.jar

EXPOSE 10000

ENTRYPOINT ["java", "-jar", "app.jar"]