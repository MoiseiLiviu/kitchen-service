FROM openjdk:11
COPY target/kitchen-service-0.0.1-SNAPSHOT.jar kitchen-service.jar
EXPOSE 8081
ENTRYPOINT exec java -jar kitchen-service.jar