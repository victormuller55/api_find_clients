# Estágio 1: build do JAR
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn
COPY src src
RUN chmod +x mvnw && ./mvnw clean package -DskipTests

# Estágio 2: runtime leve (só HTTP — Google Places API)
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx512m", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
