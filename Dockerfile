FROM openjdk:11
COPY target/kitchen-service-0.0.1-SNAPSHOT.jar kitchen-service.jar
EXPOSE ${port}
ENTRYPOINT exec java -jar kitchen-service.jar