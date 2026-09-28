# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY mvnw ./
COPY .mvn/ .mvn/
COPY pom.xml ./
RUN ./mvnw -q dependency:go-offline
COPY src/ src/
RUN ./mvnw -q clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN addgroup --system spring && adduser --system --ingroup spring spring
COPY --from=build /app/target/*.jar app.jar
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
