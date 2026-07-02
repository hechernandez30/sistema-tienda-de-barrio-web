# ============================================================
# Backend Spring Boot - build multi-stage con Java 21
# ============================================================

# --- Etapa 1: compilar el JAR ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copiamos primero el pom para aprovechar la caché de dependencias.
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copiamos el código fuente y empaquetamos (sin tests para acelerar la imagen).
COPY src ./src
RUN mvn -B clean package -DskipTests

# --- Etapa 2: imagen de ejecución ligera ---
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copiamos el JAR ejecutable generado por Spring Boot (nombre exacto del artefacto).
COPY --from=build /app/target/sistema-tienda-de-barrio-web-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
