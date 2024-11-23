# Step 1: Build Stage
FROM hseeberger/scala-sbt:8u212_1.2.8_2.12.8 as build-stage

# Set the working directory
WORKDIR /app

# Copy the SBT configuration files
COPY build.sbt project/ ./project/
COPY project/plugins.sbt project/ ./project/

# Copy the rest of the application code
COPY . .

# Build the project
RUN sbt dist

# Step 2: Production Stage
FROM openjdk:8-jre

# Define the application version
ENV VERSION=2.2.1

# Copy the built application from the build stage
COPY --from=build-stage /app/target/universal/listan-${VERSION}.zip /

# Unzip the application package
RUN unzip listan-${VERSION}.zip

# Set environment variables and expose port
ENV PATH=/listan-${VERSION}/bin:$PATH
EXPOSE 9000

# Set the working directory and entrypoint
WORKDIR /listan-${VERSION}
ENTRYPOINT ["bin/listan"]
CMD ["-Dconfig.file=conf/application.prod.conf"]
