# IRIS Replay Engine Dockerfile
# Build stage
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
# Copy only necessary files for build
COPY gradlew ./
COPY gradlew.bat ./
COPY build.gradle ./
COPY settings.gradle ./
COPY apps/replayengine ./apps/replayengine
COPY libs ./libs
COPY buildSrc ./buildSrc
COPY infra ./infra
COPY config ./config
COPY gradle ./gradle
RUN chmod +x ./gradlew
RUN ./gradlew :apps:replayengine:bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/apps/replayengine/build/libs/replayengine.jar ./replayengine.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "replayengine.jar"]
