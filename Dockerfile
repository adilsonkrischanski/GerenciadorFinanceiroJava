FROM openjdk:21-jdk-bookworm
COPY ./target/traveling-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]