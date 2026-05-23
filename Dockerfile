# Estágio 1: build do JAR
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn
COPY src src
RUN chmod +x mvnw && ./mvnw clean package -DskipTests

# Estágio 2: runtime com Chromium e dependências do Playwright (obrigatório para scraping)
FROM mcr.microsoft.com/playwright/java:v1.50.0-noble
WORKDIR /app

# Browsers já vêm na imagem; não baixar de novo no startup
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Playwright + Chromium precisam de mais memória que 256m
ENTRYPOINT ["java", "-Xmx768m", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
