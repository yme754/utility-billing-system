FROM amazoncorretto:17-alpine

RUN apk add --no-cache curl

WORKDIR /app

COPY target/auth-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java","-jar","app.jar","--spring.profiles.active=docker"]