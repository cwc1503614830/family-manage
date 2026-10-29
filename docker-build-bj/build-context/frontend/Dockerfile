# Default mirror for regions where Docker Hub is slow or unreachable.
ARG BASE_IMAGE=docker.m.daocloud.io/library/nginx:1.27-alpine
FROM ${BASE_IMAGE}

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY dist/ /usr/share/nginx/html/

# 与 prd 保持一致：容器内 nginx 监听 8091（见 nginx.conf listen 8091）。
# 宿主机对外端口由 deploy-from-harbor.sh 的 -p 参数决定（UAT 默认 8092）。
EXPOSE 8091
