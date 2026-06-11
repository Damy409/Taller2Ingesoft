FROM eclipse-temurin:21-jre
WORKDIR /app
COPY build/libs/circleguard-form-service-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8086
CMD ["java", "-jar", "app.jar"]
