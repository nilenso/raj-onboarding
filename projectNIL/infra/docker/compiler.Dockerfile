# Compiler Service Dockerfile
# Generic compiler service that can host multiple language compilers.
# Currently includes AssemblyScript toolchain (asc).

# Build stage
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle/ gradle/

# Copy source code (api needed for Gradle multi-project structure)
COPY common/ common/
COPY services/ services/

# Build the compiler service
RUN ./gradlew :services:compiler:bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:25-jre-alpine

# Install language toolchains
# AssemblyScript: requires Node.js + asc CLI + json-as for JSON support
# Note: assemblyscript must be in asc-libs for --path resolution to work correctly
RUN apk add --no-cache nodejs npm && \
    npm install -g assemblyscript && \
    mkdir -p /app/asc-libs && \
    cd /app/asc-libs && npm init -y && npm install json-as assemblyscript

WORKDIR /app

# Copy the boot JAR from builder stage
# Note: bootJar produces compiler-VERSION.jar, plain jar is compiler-VERSION-plain.jar
COPY --from=builder /app/services/compiler/build/libs/compiler-0.0.1-SNAPSHOT.jar app.jar

# Create workspace directory for compilation artifacts
RUN mkdir -p /app/tmp/compiler

# Environment defaults (can be overridden in compose)
ENV PGMQ_URL=jdbc:postgresql://postgres:5432/projectnil
ENV PGMQ_USERNAME=projectnil
ENV PGMQ_PASSWORD=projectnil
ENV ASC_BINARY=asc
ENV COMPILER_TMP_DIR=/app/tmp/compiler
ENV ASC_LIB_PATH=/app/asc-libs/node_modules

EXPOSE 8081

CMD ["java", "-jar", "app.jar"]
