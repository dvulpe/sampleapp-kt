FROM openjdk:11.0.8-jdk-slim as builder

WORKDIR /src/sampleapp-http4k

COPY . .

RUN ./gradlew jar

FROM openjdk:11.0.8-jre-slim

COPY --from=builder /src/sampleapp-http4k/build/libs/sampleapp-http4k-1.0-SNAPSHOT.jar /usr/local/bin/sampleapp.jar

ENTRYPOINT [ "java", "-jar", "/usr/local/bin/sampleapp.jar"]


