#!/usr/bin/env bash
#
# UAT 环境部署脚本（docker-build-bj）。
#
# 与 prd 环境（..\docker-build）部署在【同一台服务器 192.168.236.129】，
# 两个栈各自独立，宿主机端口错开，互不冲突：
#
#   环境        前端宿主机端口              后端宿主机端口
#   prd        8091  -> 容器 8091         12300 -> 容器 12300
#   uat (本脚本) 8092  -> 容器 8091         12301 -> 容器 12300
#
# 容器内部端口与 prd 完全一致（nginx 8091 / spring 12300），只有宿主机 publish
# 端口不同；容器名、网络名、Harbor 仓库名统一带 -uat 后缀，两个栈互不覆盖。
#
set -euo pipefail

usage() {
  SELF="$(basename "$0")"
  cat <<EOF
Usage:
  ./${SELF} [tag] [harbor-project]

UAT stack on server 192.168.236.129 (next to the prd stack):
  Frontend  http://<server-ip>:8092    (container 8091; prd stack owns host 8091)
  Backend   http://<server-ip>:12301   (container 12300; prd stack owns host 12300)

Defaults:
  tag             latest
  harbor-project  family-manage

Environment overrides:
  HARBOR_HOST, HARBOR_USER, HARBOR_PASS
  FRONTEND_PORT, BACKEND_PORT, SPRING_PROFILE
  DB_URL, DB_USER, DB_PASS
  MINIO_ENDPOINT, MINIO_BUCKET, MINIO_PUBLIC_URL

Examples:
  ./${SELF}
  ./${SELF} v1.0.0
  FRONTEND_PORT=8093 BACKEND_PORT=12302 ./${SELF}
  DB_URL='jdbc:mysql://192.168.216.140:3306/family_manage_uat?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Hongkong' ./${SELF}
EOF
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

HARBOR_HOST="${HARBOR_HOST:-192.168.236.129:80}"
HARBOR_USER="${HARBOR_USER:-admin}"
HARBOR_PASS="${HARBOR_PASS:-Harbor12345}"

TAG="${1:-latest}"
HARBOR_PROJECT="${2:-family-manage}"

NETWORK_NAME="${NETWORK_NAME:-family-manage-uat-net}"
BACKEND_NAME="${BACKEND_NAME:-family-manage-uat-backend}"
FRONTEND_NAME="${FRONTEND_NAME:-family-manage-uat-frontend}"

# 宿主机端口（与 prd 栈错开，避免端口冲突）
BACKEND_PORT="${BACKEND_PORT:-12301}"
FRONTEND_PORT="${FRONTEND_PORT:-8092}"

# 容器内部端口：与 prd 镜像保持一致，不要随意改
BACKEND_CONTAINER_PORT=12300
FRONTEND_CONTAINER_PORT=8091

SPRING_PROFILE="${SPRING_PROFILE:-uat}"

# Harbor 中的仓库名（带 -uat 后缀，不会覆盖 prd 的 family-manage-backend/frontend）
REMOTE_BACKEND="${HARBOR_HOST}/${HARBOR_PROJECT}/family-manage-uat-backend:${TAG}"
REMOTE_FRONTEND="${HARBOR_HOST}/${HARBOR_PROJECT}/family-manage-uat-frontend:${TAG}"

echo
echo "============================================================"
echo "Deploy Family Manage UAT from Harbor"
echo "Registry:  ${HARBOR_HOST}"
echo "Project:   ${HARBOR_PROJECT}"
echo "Tag:       ${TAG}"
echo "Backend:   ${REMOTE_BACKEND}"
echo "Frontend:  ${REMOTE_FRONTEND}"
echo "Profile:   ${SPRING_PROFILE}"
echo "Containers: ${BACKEND_NAME} / ${FRONTEND_NAME}"
echo "Network:    ${NETWORK_NAME}"
echo "Host ports: frontend ${FRONTEND_PORT} -> ${FRONTEND_CONTAINER_PORT}, backend ${BACKEND_PORT} -> ${BACKEND_CONTAINER_PORT}"
echo "============================================================"
echo
echo "NOTE: HTTP Harbor requires insecure-registries on this host:"
echo '  /etc/docker/daemon.json -> "insecure-registries": ["192.168.236.129:80"]'
echo "  then: sudo systemctl restart docker"
echo

if ! command -v docker >/dev/null 2>&1; then
  echo "[ERROR] docker not found."
  exit 1
fi

SERVER_IP="$(hostname -I 2>/dev/null | awk '{print $1}')"
if [[ -z "${SERVER_IP}" ]]; then
  SERVER_IP="$(ip -4 route get 1 2>/dev/null | awk '{print $7; exit}')"
fi
if [[ -z "${SERVER_IP}" ]]; then SERVER_IP="<server-ip>"; fi
echo "Detected server IPv4: ${SERVER_IP}"
echo "  Expected URLs after successful deploy:"
echo "    Frontend (UAT):  http://${SERVER_IP}:${FRONTEND_PORT}"
echo "    Backend  (UAT):  http://${SERVER_IP}:${BACKEND_PORT}"
echo

echo "[1/6] Logging in to Harbor..."
echo "${HARBOR_PASS}" | docker login "${HARBOR_HOST}" -u "${HARBOR_USER}" --password-stdin

echo "[2/6] Pulling UAT images..."
docker pull "${REMOTE_BACKEND}"
docker pull "${REMOTE_FRONTEND}"

echo "[3/6] Removing previous UAT containers (so port precheck is accurate)..."
docker rm -f "${BACKEND_NAME}" >/dev/null 2>&1 || true
docker rm -f "${FRONTEND_NAME}" >/dev/null 2>&1 || true

echo "[4/6] Host port / firewall precheck..."
port_in_use() {
  local port="$1" listeners
  if command -v ss >/dev/null 2>&1; then
    listeners="$(ss -tln 2>/dev/null || true)"
  elif command -v netstat >/dev/null 2>&1; then
    listeners="$(netstat -tln 2>/dev/null || true)"
  else
    return 1
  fi
  case "$(printf '%s' "${listeners}" | awk '{print $4}')" in
    *":${port}"*) return 0 ;;
    *) return 1 ;;
  esac
}

CONFLICT=0
for port in "${FRONTEND_PORT}" "${BACKEND_PORT}"; do
  if port_in_use "${port}"; then
    echo "  [ERROR] host port ${port} is already in use (prd stack or another service)."
    echo "          Pick another port, e.g.:  FRONTEND_PORT=8093 BACKEND_PORT=12302 ./deploy-from-harbor.sh"
    CONFLICT=1
  else
    echo "  [OK] host port ${port} is free"
  fi
done
if [[ "${CONFLICT}" -eq 1 ]]; then
  echo "[ABORT] Port conflict detected, nothing was started."
  exit 1
fi

FWD_HINT=0
if command -v firewall-cmd >/dev/null 2>&1 && firewall-cmd --state 2>/dev/null | grep -qi running; then
  for p in "${FRONTEND_PORT}:tcp" "${BACKEND_PORT}:tcp"; do
    port="${p%%:*}"; proto="${p##*:}"
    if ! firewall-cmd --zone=public --query-port="${port}/${proto}" >/dev/null 2>&1; then
      echo "  [WARN] firewalld: port ${port}/${proto} is NOT open in zone public."
      echo "         Run:  sudo firewall-cmd --permanent --add-port=${port}/${proto} && sudo firewall-cmd --reload"
      FWD_HINT=1
    fi
  done
elif command -v ufw >/dev/null 2>&1 && ufw status 2>/dev/null | grep -qi active; then
  for port in "${FRONTEND_PORT}" "${BACKEND_PORT}"; do
    if ! ufw status numbered 2>/dev/null | grep -qE "^[[:space:]]*[0-9]+.*ALLOW.*${port}(/|$|[[:space:]])"; then
      echo "  [WARN] ufw: port ${port}/tcp not explicitly ALLOWED."
      echo "         Run:  sudo ufw allow ${port}/tcp"
      FWD_HINT=1
    fi
  done
fi
if command -v getenforce >/dev/null 2>&1 && [[ "$(getenforce 2>/dev/null || true)" == "Enforcing" ]]; then
  echo "  [WARN] SELinux is Enforcing. If strange denials appear: sudo ausearch -m avc -ts recent"
fi
if [[ "${FWD_HINT}" -eq 1 ]]; then
  echo "  ^^^ 外网/局域网访问报 ERR_CONNECTION_TIMED_OUT 基本都出在上面这些防火墙规则。"
  echo "      本机 self-check 为 [OK] 但浏览器打不开 -> 执行上面的放行命令后重试。"
fi

echo "[5/6] Preparing network & starting backend..."
if ! docker network inspect "${NETWORK_NAME}" >/dev/null 2>&1; then
  docker network create "${NETWORK_NAME}"
fi

# 可选：不改镜像、只改运行期配置时用这些环境变量覆盖 application-uat.properties
BACKEND_EXTRA_ENV=()
if [[ -n "${DB_URL:-}" ]]; then
  BACKEND_EXTRA_ENV+=(-e "SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_URL=${DB_URL}")
fi
if [[ -n "${DB_USER:-}" ]]; then
  BACKEND_EXTRA_ENV+=(-e "SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_USERNAME=${DB_USER}")
fi
if [[ -n "${DB_PASS:-}" ]]; then
  BACKEND_EXTRA_ENV+=(-e "SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_PASSWORD=${DB_PASS}")
fi
if [[ -n "${MINIO_ENDPOINT:-}" ]]; then
  BACKEND_EXTRA_ENV+=(-e "MINIO_ENDPOINT=${MINIO_ENDPOINT}")
fi
if [[ -n "${MINIO_BUCKET:-}" ]]; then
  BACKEND_EXTRA_ENV+=(-e "MINIO_BUCKET=${MINIO_BUCKET}")
fi
if [[ -n "${MINIO_PUBLIC_URL:-}" ]]; then
  BACKEND_EXTRA_ENV+=(-e "MINIO_PUBLIC_URL=${MINIO_PUBLIC_URL}")
fi
if [[ "${#BACKEND_EXTRA_ENV[@]}" -gt 0 ]]; then
  echo "  Applying runtime overrides: ${BACKEND_EXTRA_ENV[*]}"
fi

docker run -d \
  --name "${BACKEND_NAME}" \
  --network "${NETWORK_NAME}" \
  --restart unless-stopped \
  -p "0.0.0.0:${BACKEND_PORT}:${BACKEND_CONTAINER_PORT}" \
  -e "SPRING_PROFILES_ACTIVE=${SPRING_PROFILE}" \
  ${BACKEND_EXTRA_ENV[@]+"${BACKEND_EXTRA_ENV[@]}"} \
  "${REMOTE_BACKEND}"

echo "[6/6] Starting frontend (nginx listens inside on ${FRONTEND_CONTAINER_PORT}; host publish on ${FRONTEND_PORT})..."
docker run -d \
  --name "${FRONTEND_NAME}" \
  --network "${NETWORK_NAME}" \
  --restart unless-stopped \
  -p "0.0.0.0:${FRONTEND_PORT}:${FRONTEND_CONTAINER_PORT}" \
  "${REMOTE_FRONTEND}"

echo
echo "[SELF-CHECK] Waiting 6s for containers to come up, then probing from THIS server (bypasses LAN firewall) ..."
sleep 6
if curl -fsS --max-time 5 -o /dev/null "http://127.0.0.1:${FRONTEND_PORT}/"; then
  echo "  [OK] UAT frontend reachable from THIS server (container nginx + docker-publish working)."
  echo "       If browser cannot open it, the cause is outside Docker:"
  echo "       firewalld/ufw (see warnings above), cloud/vm security group, or wrong IP."
else
  echo "  [FAIL] UAT frontend unreachable even from server loopback. Likely causes:"
  echo "       1) container exited:            docker ps -a | grep ${FRONTEND_NAME}"
  echo "       2) nginx config error:          docker logs ${FRONTEND_NAME}"
  echo "       3) image mismatch on listen:    docker exec ${FRONTEND_NAME} nginx -T | grep -n '^\\s*listen'"
fi
# 用受保护接口探测：只要能拿到 HTTP 状态码（401 也算）就说明后端已经起来了。
# 不用 /doc.html，因为项目未开启该接口，会误报"未就绪"。
# Spring 冷启动约 8-15s，这里最多轮询 45s，避免部署当下误报 WARN。
BACKEND_CODE=""
BACKEND_TRY=0
while [[ "${BACKEND_TRY}" -lt 23 ]]; do
  BACKEND_CODE="$(curl -sS --max-time 3 -o /dev/null -w '%{http_code}' \
    "http://127.0.0.1:${BACKEND_PORT}/api/auth/me" 2>/dev/null || true)"
  if [[ "${BACKEND_CODE}" =~ ^[1-5][0-9][0-9]$ ]]; then break; fi
  BACKEND_TRY=$(( BACKEND_TRY + 1 ))
  sleep 2
done
if [[ "${BACKEND_CODE}" =~ ^[1-5][0-9][0-9]$ ]]; then
  echo "  [OK] UAT backend is serving on port ${BACKEND_PORT} (HTTP ${BACKEND_CODE} from /api/auth/me)."
else
  echo "  [WARN] UAT backend not reachable on port ${BACKEND_PORT} after ~45s, or crashed:"
  echo "         docker logs -f ${BACKEND_NAME}"
  echo "         注意：uat profile 连接的 MySQL/MinIO 若不可达，后端起不来。"
  echo "         可在部署时用 DB_URL/MINIO_ENDPOINT 覆盖，例如："
  echo "         DB_URL='jdbc:mysql://192.168.216.140:3306/family_manage_uat?...' ./deploy-from-harbor.sh"
fi
echo
echo "============================================================"
echo "[OK] UAT deploy step finished. Open these URLs FROM YOUR PC BROWSER:"
echo "    Frontend (UAT):  http://${SERVER_IP}:${FRONTEND_PORT}"
echo "    Backend  (UAT):  http://${SERVER_IP}:${BACKEND_PORT}"
echo ""
echo "prd stack keeps running untouched on:"
echo "    Frontend (prd):  http://${SERVER_IP}:8091"
echo "    Backend  (prd):  http://${SERVER_IP}:12300"
echo "============================================================"
echo "If the browser fails while self-check printed [OK] for frontend:"
echo "  1) Run the firewall allow commands printed earlier and refresh."
echo "  2) If this VM is behind VMware/AWS/阿里云 security group, allow inbound TCP:"
echo "     ports ${FRONTEND_PORT} and ${BACKEND_PORT} (source: your LAN subnet or 0.0.0.0/0)"
echo "  3) Confirm raw TCP reachability from Windows PowerShell:"
echo "     Test-NetConnection ${SERVER_IP} -Port ${FRONTEND_PORT}"
echo
docker ps --filter "name=${BACKEND_NAME}" --filter "name=${FRONTEND_NAME}" \
  --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
