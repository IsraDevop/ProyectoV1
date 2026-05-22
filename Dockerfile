FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -q clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S yala && adduser -S yala -G yala
WORKDIR /app
COPY --from=build /workspace/target/yala-0.0.1-SNAPSHOT.jar app.jar

USER yala
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
