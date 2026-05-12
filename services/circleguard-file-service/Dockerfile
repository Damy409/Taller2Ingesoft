FROM gradle:8-jdk21 AS build
WORKDIR /workspace
COPY . .
ARG GRADLE_PROJECT
ARG SERVICE_PATH
RUN chmod +x ./gradlew && ./gradlew ${GRADLE_PROJECT}:bootJar -x test && cp ${SERVICE_PATH}/build/libs/*.jar /app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app.jar app.jar
ARG SERVICE_PORT=8080
EXPOSE ${SERVICE_PORT}
CMD ["java", "-jar", "app.jar"]

