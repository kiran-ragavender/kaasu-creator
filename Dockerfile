FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:25-jre-jammy
WORKDIR /app
COPY --from=build /app/target/kaasu-creator-0.0.1-SNAPSHOT.jar app.jar
# Render injects PORT at runtime — Spring Boot reads it via ${PORT:9093}
EXPOSE 10000
ENTRYPOINT ["java", "-Xmx400m", "-Xms128m", "-jar", "app.jar"]
