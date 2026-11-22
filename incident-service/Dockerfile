# Stage 1: Build JAR using Maven
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /build

COPY pom.xml mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY common-dto common-dto
COPY incident-service incident-service

RUN chmod +x mvnw && ./mvnw clean package -pl incident-service -am -DskipTests

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder /build/incident-service/target/*.jar app.jar
EXPOSE 8084
ENV SPRING_PROFILES_ACTIVE=docker
HEALTHCHECK --interval=15s --timeout=5s --retries=5 --start-period=30s \
  CMD wget -qO- http://localhost:8084/actuator/health | grep -q 'UP' || exit 1
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
