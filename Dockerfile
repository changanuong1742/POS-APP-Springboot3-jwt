#FROM openjdk:17
#ARG JAR_FILE=target/*.jar
#COPY ${JAR_FILE} app-0.0.1-SNAPSHOT.jar
#ENTRYPOINT ["java", "-jar", "/app-0.0.1-SNAPSHOT.jar"]
#
# Build stage
FROM maven:3.8.4-openjdk-17 AS build

# Set the working directory in the container
WORKDIR /app

# Copy the source code into the container
COPY . /app

# Build the application with Maven
RUN mvn clean package

# Create a new image for the runtime stage
FROM openjdk:17

# Set the working directory in the container
WORKDIR /app

# Copy the JAR file from the build stage to the runtime stage
COPY --from=build /app/target/*.jar /app/my-app.jar

# Run the application
CMD ["java", "-jar", "/app/my-app.jar"]
