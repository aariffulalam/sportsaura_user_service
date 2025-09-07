FROM eclipse-temurin:24-jdk-alpine
WORKDIR /app
COPY target/user-service-1.0.0.jar user-service.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","user-service.jar"]
