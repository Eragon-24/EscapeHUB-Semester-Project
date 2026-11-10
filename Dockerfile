FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
COPY config ./config
RUN mvn -B -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --uid 10001 spring
COPY --from=build /workspace/target/escapehub-0.1.0-SNAPSHOT.jar /app/escapehub.jar
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/escapehub.jar"]
