FROM eclipse-temurin:21-jre
WORKDIR /app
COPY build/libs/circleguard-promotion-service-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8088
CMD ["java", "-jar", "app.jar"]
