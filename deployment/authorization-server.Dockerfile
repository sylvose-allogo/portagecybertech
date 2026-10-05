FROM maven:3.9.9-eclipse-temurin-23 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY authorization-server/pom.xml ./authorization-server/pom.xml
COPY resource-server/pom.xml ./resource-server/pom.xml
COPY integration-tests/pom.xml ./integration-tests/pom.xml
COPY authorization-server/src ./authorization-server/src

RUN mvn -B -ntp -pl authorization-server -am -DskipTests package

FROM eclipse-temurin:23-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --home-dir /app app
WORKDIR /app
COPY --from=build --chown=10001:10001 /workspace/authorization-server/target/authorization-server-1.0.0-exec.jar /app/app.jar
USER 10001:10001
EXPOSE 9090
HEALTHCHECK --interval=15s --timeout=3s --start-period=45s --retries=5 \
    CMD curl --fail --silent http://127.0.0.1:9090/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.io.tmpdir=/tmp", "-jar", "/app/app.jar"]
