# Multi-stage build for MenuMind AI Spring Boot Backend

# -------------------------------------------------------------
# Stage 1: Build stage with Maven and Eclipse Temurin JDK 17
# -------------------------------------------------------------
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy pom.xml and dependency descriptors first to leverage Docker layer caching
COPY pom.xml .

# Download dependencies offline (cache layer)
RUN mvn dependency:go-offline -B

# Copy project source code
COPY src ./src

# Package application jar (skip tests for build efficiency during image creation)
RUN mvn clean package -DskipTests -B

# -------------------------------------------------------------
# Stage 2: Minimal Production Runtime with Eclipse Temurin JRE 17
# -------------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Create a non-privileged system user for security
RUN groupadd -r appgroup && useradd -r -g appgroup -d /app appuser

# Copy built JAR artifact from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Set ownership to the non-privileged user
RUN chown -R appuser:appgroup /app

USER appuser

# Render dynamically assigns PORT at runtime (defaulting to 8080)
ENV PORT=8080
EXPOSE ${PORT}

# Configure memory efficiency and pass PORT to Spring Boot
ENTRYPOINT ["sh", "-c", "java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Dserver.port=${PORT} -jar app.jar"]
