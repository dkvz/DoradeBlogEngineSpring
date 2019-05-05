# The plan is to build this image
# Run it, with --rm -it
# Build the app with ./mvnw package
# Copy the target build from the running container:
# docker cp <containerId>:/usr/src/myapp/target/DoradeBlogEngineSpring-0.0.1-SNAPSHOT.jar ./build/

FROM openjdk:8u212-jdk

COPY . /usr/src/myapp
WORKDIR /usr/src/myapp

CMD ["bash"]