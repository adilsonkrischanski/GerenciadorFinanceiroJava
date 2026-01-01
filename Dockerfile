### ===== STAGE 1 - BUILD =====
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copia somente o pom para cache das dependências
COPY pom.xml .
RUN mvn -B dependency:resolve

# Copia o restante do código
COPY src ./src

# Compila a aplicação
RUN mvn clean package -DskipTests


### ===== STAGE 2 - RUN =====
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copia o JAR gerado
COPY --from=build /app/target/*SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
