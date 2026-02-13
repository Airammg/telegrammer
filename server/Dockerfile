FROM gradle:8.11-jdk17 AS build
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY server/build.gradle.kts server/
COPY server/src server/src
# Create empty shared/androidApp dirs so settings.gradle doesn't fail
RUN mkdir -p shared/src/commonMain androidApp/src/main
COPY shared/build.gradle.kts shared/
COPY androidApp/build.gradle.kts androidApp/
RUN gradle :server:buildFatJar --no-daemon

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/server/build/libs/telegrammer-server-all.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
