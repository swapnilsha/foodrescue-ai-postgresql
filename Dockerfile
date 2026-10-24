# ==========================================
# STAGE 1: Build using Maven
# ==========================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# Copy project configurations
COPY pom.xml .

# Pre-fetch dependencies to speed up subsequent builds
RUN mvn dependency:go-offline -B

# Copy application source files
COPY src ./src

# Build the project (skip tests since MySQL database is not available during build)
RUN mvn package -DskipTests -B

# ==========================================
# STAGE 2: Lightweight Runtime JRE Image
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a non-root system group and user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Create uploads directory and change ownership to spring user
RUN mkdir -p /app/uploads && chown -R spring:spring /app/uploads
VOLUME /app/uploads

# Copy executable jar from builder stage
COPY --from=builder /build/target/*.jar app.jar
RUN chown spring:spring app.jar

# Switch to the non-root user
USER spring:spring

# Expose default application port
EXPOSE 8080

# Set JVM memory options to respect container memory limits
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
