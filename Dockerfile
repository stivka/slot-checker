# This jdk is based on Debian 10 (buster) and should have the necessary utilities like apt-get, curl, and zip
FROM openjdk:17-jdk-buster AS build

# Set up environment variables for Gradle
ENV GRADLE_HOME=/opt/gradle
ENV GRADLE_USER_HOME=/gradle
ENV PATH=$PATH:$GRADLE_HOME/bin

# Install necessary utilities
RUN apt-get update && apt-get install -y curl unzip wget libnss3

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

# Copy the build.gradle, settings.gradle, and source code to the container
COPY ./build.gradle ./build.gradle
COPY ./settings.gradle ./settings.gradle
COPY ./src ./src

# Download and extract chrome and chromedriver
RUN wget https://edgedl.me.gvt1.com/edgedl/chrome/chrome-for-testing/117.0.5938.92/linux64/chrome-linux64.zip \
    && unzip chrome-linux64.zip -d /build \
    && chmod +x /build/chrome-linux64/chrome
    
RUN wget https://edgedl.me.gvt1.com/edgedl/chrome/chrome-for-testing/117.0.5938.92/linux64/chromedriver-linux64.zip \
    && unzip chromedriver-linux64.zip -d /build \
    && chmod +x /build/chromedriver-linux64/chromedriver

# Build the project
RUN gradle build -x test

# Specify the base image for the runtime environment and start the run stage with its' own filesystem
FROM openjdk:17-jdk-slim

RUN apt-get update && apt-get install -y libnss3

# Set the working directory for the runtime environment
WORKDIR /app

# Copy the built jar, chrome binaries, and chromedriver from the build stage
COPY --from=build /build/build/libs/slot-checker-0.0.1-SNAPSHOT.jar /app/slot-checker.jar
COPY --from=build /build/chrome-linux64/chrome /app/chrome-linux64/chrome
COPY --from=build /build/chromedriver-linux64/chromedriver /app/chromedriver-linux64/chromedriver

# Run the application
ENTRYPOINT ["java", "-jar", "/app/slot-checker.jar"]
