FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/TireVaultRestApi-0.0.1-SNAPSHOT.war app.war
EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.war"]