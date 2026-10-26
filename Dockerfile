FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY . .

RUN mvn -B -DskipTests package


FROM eclipse-temurin:21-jre-noble

WORKDIR /app

COPY --from=build /workspace/fms-app/target/fms-app-0.0.1-SNAPSHOT.jar /app/app.jar

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]