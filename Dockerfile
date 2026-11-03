# syntax=docker/dockerfile:1.7

# Build stage
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

ARG MODULE

COPY . .

RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -B -pl ${MODULE} -am -DskipTests package

RUN mkdir -p /app && \
    JAR_FILE="$(find /workspace/${MODULE}/target -maxdepth 1 -type f -name '*.jar' ! -name '*.original' ! -name '*-sources.jar' ! -name '*-javadoc.jar' | head -n 1)" && \
    test -n "$JAR_FILE" && \
    cp "$JAR_FILE" /app/app.jar

# Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN useradd --system --uid 10001 appuser
RUN mkdir -p /var/lib/nexushr/documents \
    && chown -R appuser:appuser /var/lib/nexushr

COPY --from=build --chown=appuser:appuser /app/app.jar /app/app.jar

USER appuser

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
    CMD curl --fail --silent http://localhost:${SERVER_PORT:-8080}/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
