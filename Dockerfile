FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY src src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system bitalep && useradd --system --gid bitalep --create-home --home-dir /app bitalep \
    && mkdir -p /data/files /app && chown -R bitalep:bitalep /app /data
WORKDIR /app
COPY --from=build --chown=bitalep:bitalep /src/target/bitalep-api-1.0.0.jar /app/app.jar
USER bitalep
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=8 \
  CMD curl -sf http://127.0.0.1:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
