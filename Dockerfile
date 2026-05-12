FROM eclipse-temurin:21-jre
WORKDIR /app
COPY services/circleguard-auth-service/build/libs/circleguard-auth-service-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8180
CMD ["java", "-jar", "app.jar"]
