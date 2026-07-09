# Stage 1: Build the Maven application
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy the pom.xml and fetch dependencies first for efficient Docker layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build package
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: Create a minimal JRE runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy built jar from stage 1
COPY --from=build /app/target/codeduel-0.0.1-SNAPSHOT.jar app.jar

# Expose backend REST & WebSocket port
EXPOSE 8080

# Run JVM with garbage collection and memory tuning flags suited for container environments
ENTRYPOINT ["java", "-XX:+UseG1GC", "-jar", "app.jar"]
