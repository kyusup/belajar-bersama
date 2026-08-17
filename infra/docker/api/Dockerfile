# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY apps/api/mvnw apps/api/mvnw.cmd apps/api/pom.xml ./apps/api/
COPY apps/api/.mvn ./apps/api/.mvn

RUN cd apps/api && chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY apps/api/src ./apps/api/src

RUN cd apps/api && ./mvnw -B -q package -DskipTests -Dquarkus.package.type=fast-jar

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN apt-get update \
  && apt-get install -y --no-install-recommends curl \
  && rm -rf /var/lib/apt/lists/*

COPY --from=build /workspace/apps/api/target/quarkus-app /app

EXPOSE 8080
USER 10001

HEALTHCHECK --interval=10s --timeout=5s --retries=12 \
  CMD curl -fsS http://localhost:8080/api/v1/health || exit 1

CMD ["java", "-jar", "quarkus-run.jar"]
