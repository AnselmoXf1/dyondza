# Estágio 1: Build com Gradle e JDK 17
FROM gradle:8.10-jdk17 AS build
WORKDIR /app
COPY .. .
RUN gradle :backend:installDist --no-daemon

# Estágio 2: Runtime leve com OpenJDK 17
FROM openjdk:17-slim
WORKDIR /app
COPY --from=build /app/backend/build/install/backend /app

# Variáveis de Ambiente esperadas no Cloud Run
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["/app/bin/backend"]
