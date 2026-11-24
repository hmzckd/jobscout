FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src/ src/
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 jobscout
WORKDIR /app
COPY --from=build /src/target/jobscout-0.0.1-SNAPSHOT.jar app.jar
USER jobscout
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
