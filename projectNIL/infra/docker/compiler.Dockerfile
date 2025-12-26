# Compiler Service Dockerfile
# Generic compiler service that can host multiple language compilers.
# Currently includes AssemblyScript toolchain (asc).

# Build stage
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle/ gradle/

# Copy source code
COPY common/ common/
COPY services/compiler/ services/compiler/

# Build the compiler service
RUN ./gradlew :services:compiler:bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:25-jre-alpine

# Install language toolchains
# AssemblyScript: requires Node.js + asc CLI
RUN apk add --no-cache nodejs npm && \
    npm install -g assemblyscript

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/services/compiler/build/libs/*.jar app.jar

# Create workspace directory for compilation artifacts
RUN mkdir -p /app/tmp/compiler

# Environment defaults (can be overridden in compose)
ENV PGMQ_URL=jdbc:postgresql://postgres:5432/projectnil
ENV PGMQ_USERNAME=projectnil
ENV PGMQ_PASSWORD=projectnil
ENV ASC_BINARY=asc
ENV COMPILER_TMP_DIR=/app/tmp/compiler

EXPOSE 8081

CMD ["java", "-jar", "app.jar"]
