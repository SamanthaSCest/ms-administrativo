# Build (contexto = raiz do repositório):
#   docker build -f ms-administrativo/Dockerfile -t clinical/ms-administrativoatest .
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -q -pl ms-administrativo -am -DskipTests package

FROM eclipse-temurin:25-jre-noble
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --no-create-home app
WORKDIR /app
COPY --from=build /workspace/ms-administrativo/target/*.jar app.jar
USER app
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
