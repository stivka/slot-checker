# Use openjdk with Debian 10 (buster) which has utilities like apt-get, curl, and zip
FROM openjdk:17-jdk-buster AS build

# Set up environment variables for Gradle
ENV GRADLE_HOME=/opt/gradle
ENV GRADLE_USER_HOME=/gradle
ENV PATH=$PATH:$GRADLE_HOME/bin

# Install necessary utilities
RUN apt-get update && apt-get install -y curl unzip

# Setting up Gradle
RUN mkdir /opt/gradle && \
    curl -L -o gradle-8.2.1-bin.zip https://services.gradle.org/distributions/gradle-8.2.1-bin.zip && \
    unzip gradle-8.2.1-bin.zip -d /opt/gradle && \
    ln -s /opt/gradle/gradle-8.2.1 /opt/gradle/latest && \
    rm gradle-8.2.1-bin.zip

# Update PATH to include Gradle and make it executable
ENV PATH=$PATH:/opt/gradle/latest/bin
RUN chmod +x /opt/gradle/latest/bin/gradle

# Set the working directory for the build stage
WORKDIR /build

# Copy the build.gradle and settings.gradle first for caching
# if this line is before copying the src line then if no changes were made to dependencies 
# and say only to src code, then libraries won't be re-downloaded, speeding up the build process.
# This is called layer-based caching system in Docker
COPY ./build.gradle ./settings.gradle ./

# Copy the source code afterwards
COPY ./src ./src

# Build the project
RUN gradle build -x test

# Specify the base image for the runtime environment and start the run stage with its own filesystem
FROM openjdk:17-jdk-slim

# Set the working directory for the runtime environment
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /build/build/libs/slot-checker-0.0.1-SNAPSHOT.jar /app/slot-checker.jar

# Run the application
ENTRYPOINT ["java", "-jar", "/app/slot-checker.jar"]
