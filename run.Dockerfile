# Version on the server is 8u102
# Stretch is the wrong distribution but I guess it's close enough?
# I had to use u212.

# docker build -f run.Dockerfile -t dorade-api .
# docker run --rm -p 9001:9001 dorade-api

#FROM openjdk:8u212-jdk-stretch
FROM openjdk:11.0.3-jdk-stretch

COPY ./target/DoradeBlogEngineSpring-0.0.1-SNAPSHOT.jar /usr/src/myapp/
COPY ./*.sqlite /usr/src/myapp/
COPY ./words.txt /usr/src/myapp/
COPY ./*.mmdb /usr/src/myapp/

WORKDIR /usr/src/myapp
RUN mkdir import && apt-get update && apt-get install -y sqlite3

CMD ["java", "-jar", "DoradeBlogEngineSpring-0.0.1-SNAPSHOT.jar"]