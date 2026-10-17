# syntax=docker/dockerfile:1

# Step 1: build the JAR with Maven
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests

# Step 2: run it on a small Java runtime image, as a non-root user
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --no-create-home --uid 1001 app
COPY --from=build /app/target/user-service.jar app.jar
USER app
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
