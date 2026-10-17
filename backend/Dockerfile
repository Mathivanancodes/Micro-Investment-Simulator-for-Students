FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -ntp package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd --system simulator && useradd --system --gid simulator simulator
COPY --from=build --chown=simulator:simulator /app/target/simulator-0.0.1-SNAPSHOT.jar app.jar
USER simulator
EXPOSE 8080
ENTRYPOINT ["java", "-Duser.timezone=UTC", "-jar", "app.jar"]
