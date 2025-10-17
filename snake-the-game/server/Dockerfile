# Multi-stage build: compile the Spring Boot server and run it on a slim JRE
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy parent and module POMs first to leverage Docker layer caching
COPY snake-the-game/pom.xml snake-the-game/pom.xml
COPY snake-the-game/core/pom.xml snake-the-game/core/pom.xml
COPY snake-the-game/launcher/pom.xml snake-the-game/launcher/pom.xml
COPY snake-the-game/server/pom.xml snake-the-game/server/pom.xml

RUN mvn -f snake-the-game/pom.xml -pl server -am dependency:go-offline

# Copy the full sources and build the server module
COPY snake-the-game snake-the-game
RUN mvn -f snake-the-game/pom.xml -pl server -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/snake-the-game/server/target/snake-server.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
