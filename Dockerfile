# This jdk is based on Debian 10 (buster) and should have the necessary utilities like apt-get, curl and zip
FROM openjdk:17-jdk-buster AS build

# Set up environment variables for Gradle
ENV GRADLE_HOME=/opt/gradle
ENV GRADLE_USER_HOME=/gradle
ENV PATH=$PATH:$GRADLE_HOME/bin

# Install necessary utilities
RUN apt-get update && apt-get install -y curl unzip

# Echo what's being done for better debugging
RUN echo "Setting up Gradle..." && \
    mkdir /opt/gradle && \
    curl -L -o gradle-8.2.1-bin.zip https://services.gradle.org/distributions/gradle-8.2.1-bin.zip && \
    unzip gradle-8.2.1-bin.zip -d /opt/gradle && \
    ln -s /opt/gradle/gradle-8.2.1 /opt/gradle/latest && \
    rm gradle-8.2.1-bin.zip

# Update PATH to include Gradle and make it executable
ENV PATH=$PATH:/opt/gradle/latest/bin
RUN chmod +x /opt/gradle/latest/bin/gradle

# Set the working directory in the container
WORKDIR /app

# Copy the build.gradle, settings.gradle, and source code to the container
COPY ./build.gradle ./build.gradle
COPY ./settings.gradle ./settings.gradle
COPY ./src ./src

# Build the project
RUN gradle build -x test

# Specify the base image for the runtime environment
FROM openjdk:17

WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/build/libs/slot-checker-0.0.1-SNAPSHOT.jar /slot-checker.jar

# Run the application
ENTRYPOINT ["java", "-jar", "/slot-checker.jar"]
