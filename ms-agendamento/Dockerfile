# Build (contexto = raiz do repositório):
#   docker build -f ms-agendamento/Dockerfile -t clinical/ms-agendamentoatest .
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -q -pl ms-agendamento -am -DskipTests package

FROM eclipse-temurin:17-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --no-create-home app
WORKDIR /app
COPY --from=build /workspace/ms-agendamento/target/*.jar app.jar
USER app
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
