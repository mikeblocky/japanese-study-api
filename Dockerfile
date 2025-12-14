# Build stage - optimized with dependency caching
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy pom.xml first - this layer is cached unless pom.xml changes
COPY pom.xml .

# Download all dependencies - cached layer (only re-runs if pom.xml changes)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build with parallel execution for faster compilation
RUN mvn clean package -DskipTests -T 1C --batch-mode

# Run stage - minimal JRE image with CDS for faster startup
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy only the built JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Create Class Data Sharing (CDS) archive for faster JVM startup
# This pre-loads commonly used classes into a shared archive
RUN java -Xshare:dump -XX:SharedClassListFile=/dev/null -XX:SharedArchiveFile=/app/app-cds.jsa -jar app.jar --spring.main.lazy-initialization=true --spring.jpa.hibernate.ddl-auto=none || true

# Expose port
EXPOSE 8080

# Optimized JVM flags for Render (constrained memory environment)
# -XX:+UseG1GC: Low-latency garbage collector
# -XX:+UseStringDeduplication: Reduce memory footprint
# -Xshare:on: Use CDS archive for faster class loading
# -XX:MaxRAMPercentage: Use container-aware memory limits
ENTRYPOINT ["java", \
    "-XX:+UseG1GC", \
    "-XX:+UseStringDeduplication", \
    "-XX:MaxRAMPercentage=80.0", \
    "-Xshare:auto", \
    "-Dspring.profiles.active=prod", \
    "-jar", "app.jar"]
