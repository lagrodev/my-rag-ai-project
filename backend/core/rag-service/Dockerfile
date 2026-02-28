# ─── Build Stage ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /workspace

# Copy Gradle wrapper (from backend/ context root)
COPY gradlew gradlew.bat ./
COPY gradle/ gradle/

# Copy rag-service sources (from backend/core/rag-service/)
COPY core/rag-service/ ./

RUN chmod +x gradlew

# Download dependencies first (better layer caching)
RUN ./gradlew dependencies --no-daemon --quiet || true

# Build the fat JAR, skipping tests
RUN ./gradlew :app:bootJar -x test --no-daemon

# ─── Runtime Stage ────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre-alpine AS runtime

WORKDIR /app

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy JAR from builder
COPY --from=builder /workspace/app/build/libs/*.jar app.jar

# Switch to non-root user
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
