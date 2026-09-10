# Default mirror for regions where Docker Hub is slow or unreachable.
ARG BASE_IMAGE=docker.m.daocloud.io/library/eclipse-temurin:8-jre
FROM ${BASE_IMAGE}

WORKDIR /app
COPY app.jar /app/app.jar

ENV SPRING_PROFILES_ACTIVE=dev

EXPOSE 12300

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
