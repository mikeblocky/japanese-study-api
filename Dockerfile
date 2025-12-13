# Build stage - optimized with dependency caching
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy pom.xml first - this layer is cached unless pom.xml changes
COPY pom.xml .

# Download all dependencies - cached layer (only re-runs if pom.xml changes)
RUN mvn dependency:go-offline -B

# Copy source code and Maven config
COPY src ./src
COPY .mvn ./.mvn

# Build with parallel execution for faster compilation
# -T 1C uses one thread per CPU core
# --batch-mode for non-interactive build
RUN mvn clean package -DskipTests \
    -T 1C \
    --batch-mode \
    -Dmaven.compiler.useIncrementalCompilation=false

# Run stage - minimal JRE image for smaller size and faster deployment
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy only the built JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Run with production profile
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
