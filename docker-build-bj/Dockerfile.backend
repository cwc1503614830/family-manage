# Default mirror for regions where Docker Hub is slow or unreachable.
ARG BASE_IMAGE=docker.m.daocloud.io/library/eclipse-temurin:8-jre
FROM ${BASE_IMAGE}

WORKDIR /app
COPY app.jar /app/app.jar

# UAT 环境激活 application-uat.properties。
# 运行期仍可用 docker run -e SPRING_PROFILES_ACTIVE=... 覆盖。
ENV SPRING_PROFILES_ACTIVE=uat

# 容器内端口与 prd 保持一致（12300），宿主机 publish 端口由 deploy 脚本决定（UAT 默认 12301）。
EXPOSE 12300

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
