# Use OpenJDK as base image
FROM openjdk:21-slim

# Set the working directory inside the container
WORKDIR /app

# Copy built jar into the container
COPY target/digest-*.jar digest.jar

# Expose port
EXPOSE 8080

# Run the app
ENTRYPOINT ["java", "-jar", "digest.jar"]
