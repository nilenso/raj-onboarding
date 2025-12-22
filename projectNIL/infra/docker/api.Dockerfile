# Stage 1: Build
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /app

# Copy the whole project to handle multi-module dependencies
COPY . .

# Build the api service
RUN ./gradlew :services:api:bootJar --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Copy the built jar from the builder stage
COPY --from=builder /app/services/api/build/libs/api-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
