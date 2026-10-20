# Multi-stage build for the StudyHive Spring Boot backend.
#
# Build context must be the REPO ROOT (not backend/), because this is a
# multi-module Maven reactor and the backend module needs the parent pom.xml.
# On Render: set "Dockerfile Path" to backend/Dockerfile and leave the Docker
# build context as the repo root.
#
#   docker build -f backend/Dockerfile -t studyhive-backend .

# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy only the POMs first so dependency resolution is cached across builds
# that only change source files.
COPY pom.xml .
COPY backend/pom.xml backend/pom.xml
COPY desktop/pom.xml desktop/pom.xml
RUN mvn -q -pl backend -am -DskipTests dependency:go-offline || true

COPY backend backend
RUN mvn -q -pl backend -am -DskipTests clean package

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Run as a non-root user.
RUN useradd --system --no-create-home --shell /usr/sbin/nologin appuser
COPY --from=build /workspace/backend/target/backend.jar app.jar
RUN chown appuser:appuser app.jar
USER appuser

# No EXPOSE'd/hardcoded port: the app binds to $PORT (Render injects this at
# runtime; application.yml falls back to SERVER_PORT, then 8080). All other
# configuration -- database, JWT secret, storage provider, CORS origin --
# comes from environment variables set on the host, never baked into the image.
ENTRYPOINT ["java", "-jar", "app.jar"]
