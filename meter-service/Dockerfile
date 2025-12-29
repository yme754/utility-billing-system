FROM amazoncorretto:17-alpine

RUN apk add --no-cache curl

WORKDIR /app

COPY target/meter-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java","-jar","app.jar","--spring.profiles.active=docker"]