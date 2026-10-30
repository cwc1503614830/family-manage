#!/usr/bin/env bash
#
# Family Manage —— 从 Harbor 部署（公共引擎，支持 prd / uat / dev）
#
# 一般不要直接调用本脚本，请使用同目录下的三个入口：
#   ./deploy-prd-from-harbor.sh <tag>    生产环境（内部使用）
#   ./deploy-uat-from-harbor.sh <tag>    UAT 环境（外部用户使用）
#   ./deploy-dev-from-harbor.sh <tag>    开发环境（内部使用）
#
# 三套环境部署在同一台服务器（192.168.236.129）上，共用同一批镜像
# （同一个 jar、同一份前端产物），靠下面四个维度隔离，互不影响：
#
#   环境  profile  前端 宿主机端口/容器端口   后端 宿主机端口/容器端口   容器名后缀   独立 docker 网络
#   prd   prd      8091   / 8091             12300  / 12300             (无)         family-manage-net
#   uat   uat      8092   / 8091             12301  / 12300             -uat         family-manage-uat-net
#   dev   dev      8093   / 8091             12302  / 12300             -dev         family-manage-dev-net
#
# 数据库 / MinIO 由各自的 application-<env>.properties 决定，也可在部署时用
# DB_URL / DB_USER / DB_PASS / MINIO_* 覆盖（不需要重新打镜像）。
#
set -euo pipefail

STACK_ENV="${STACK_ENV:-prd}"
case "${STACK_ENV}" in
  prd) ENV_TITLE="生产环境 (prd)" ;;
  uat) ENV_TITLE="UAT 环境 (uat, 外部用户)" ;;
  dev) ENV_TITLE="开发环境 (dev)" ;;
  *)
    echo "[ERROR] STACK_ENV 只能是 prd / uat / dev，当前值：${STACK_ENV}"
    exit 1
    ;;
esac

usage() {
  SELF="$(basename "$0")"
  cat <<EOF
Usage:
  STACK_ENV=<prd|uat|dev> ./${SELF} [tag] [harbor-project]

推荐用法（同目录下的三个入口脚本，已自动指定环境）：
  ./deploy-prd-from-harbor.sh [tag] [harbor-project]     生产环境  8091 / 12300
  ./deploy-uat-from-harbor.sh [tag] [harbor-project]     UAT 环境  8092 / 12301
  ./deploy-dev-from-harbor.sh [tag] [harbor-project]     开发环境  8093 / 12302

默认值:
  tag             latest
  harbor-project  family-manage

运行期覆盖（都不需要重新打镜像）:
  HARBOR_HOST, HARBOR_USER, HARBOR_PASS
  FRONTEND_PORT, BACKEND_PORT, SPRING_PROFILE
  NETWORK_NAME, BACKEND_NAME, FRONTEND_NAME
  DB_URL, DB_USER, DB_PASS
  MINIO_ENDPOINT, MINIO_BUCKET, MINIO_PUBLIC_URL

示例:
  ./deploy-uat-from-harbor.sh v1.2.0
  FRONTEND_PORT=8094 BACKEND_PORT=12303 ./deploy-uat-from-harbor.sh
  DB_URL='jdbc:mysql://192.168.216.140:3306/family_manage_uat?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Hongkong' ./deploy-uat-from-harbor.sh
EOF
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

# ---------- 各环境默认值（可由环境变量覆盖） ----------
case "${STACK_ENV}" in
  prd) DEF_FRONTEND_PORT=8091; DEF_BACKEND_PORT=12300; DEF_SUFFIX="" ;;
  uat) DEF_FRONTEND_PORT=8092; DEF_BACKEND_PORT=12301; DEF_SUFFIX="-uat" ;;
  dev) DEF_FRONTEND_PORT=8093; DEF_BACKEND_PORT=12302; DEF_SUFFIX="-dev" ;;
esac

HARBOR_HOST="${HARBOR_HOST:-192.168.236.129:80}"
HARBOR_USER="${HARBOR_USER:-admin}"
HARBOR_PASS="${HARBOR_PASS:-Harbor12345}"

TAG="${1:-latest}"
HARBOR_PROJECT="${2:-family-manage}"

FRONTEND_PORT="${FRONTEND_PORT:-${DEF_FRONTEND_PORT}}"
BACKEND_PORT="${BACKEND_PORT:-${DEF_BACKEND_PORT}}"
SPRING_PROFILE="${SPRING_PROFILE:-${STACK_ENV}}"

NETWORK_NAME="${NETWORK_NAME:-family-manage${DEF_SUFFIX}-net}"
BACKEND_NAME="${BACKEND_NAME:-family-manage${DEF_SUFFIX}-backend}"
FRONTEND_NAME="${FRONTEND_NAME:-family-manage${DEF_SUFFIX}-frontend}"

# 容器内部端口：三套环境统一，与镜像保持一致，不要随意改
BACKEND_CONTAINER_PORT=12300
FRONTEND_CONTAINER_PORT=8091

# 三套环境共用同一批镜像
REMOTE_BACKEND="${HARBOR_HOST}/${HARBOR_PROJECT}/family-manage-backend:${TAG}"
REMOTE_FRONTEND="${HARBOR_HOST}/${HARBOR_PROJECT}/family-manage-frontend:${TAG}"

echo
echo "============================================================"
echo "Deploy Family Manage —— ${ENV_TITLE}"
echo "Registry:   ${HARBOR_HOST}"
echo "Project:    ${HARBOR_PROJECT}"
echo "Tag:        ${TAG}"
echo "Profile:    ${SPRING_PROFILE}"
echo "Backend:    ${REMOTE_BACKEND}"
echo "Frontend:   ${REMOTE_FRONTEND}"
echo "Containers: ${BACKEND_NAME} / ${FRONTEND_NAME}"
echo "Network:    ${NETWORK_NAME}"
echo "Host ports: frontend ${FRONTEND_PORT} -> ${FRONTEND_CONTAINER_PORT}, backend ${BACKEND_PORT} -> ${BACKEND_CONTAINER_PORT}"
echo "============================================================"
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
echo "  This environment will be reachable at:"
echo "    Frontend: http://${SERVER_IP}:${FRONTEND_PORT}"
echo "    Backend:  http://${SERVER_IP}:${BACKEND_PORT}"
echo

echo "[1/6] Logging in to Harbor..."
echo "${HARBOR_PASS}" | docker login "${HARBOR_HOST}" -u "${HARBOR_USER}" --password-stdin

echo "[2/6] Pulling images..."
docker pull "${REMOTE_BACKEND}"
docker pull "${REMOTE_FRONTEND}"

echo "[3/6] Removing previous ${STACK_ENV} containers (so the port precheck is accurate)..."
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
    echo "  [ERROR] host port ${port} is already in use by another stack/service."
    echo "          查看占用： ss -tlnp | grep :${port}"
    echo "          换端口部署示例： FRONTEND_PORT=8094 BACKEND_PORT=12303 ./$0"
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
      echo "         Run:  firewall-cmd --permanent --add-port=${port}/${proto} && firewall-cmd --reload"
      FWD_HINT=1
    fi
  done
elif command -v ufw >/dev/null 2>&1 && ufw status 2>/dev/null | grep -qi active; then
  for port in "${FRONTEND_PORT}" "${BACKEND_PORT}"; do
    if ! ufw status numbered 2>/dev/null | grep -qE "^[[:space:]]*[0-9]+.*ALLOW.*${port}(/|$|[[:space:]])"; then
      echo "  [WARN] ufw: port ${port}/tcp not explicitly ALLOWED."
      echo "         Run:  ufw allow ${port}/tcp"
      FWD_HINT=1
    fi
  done
fi
if command -v getenforce >/dev/null 2>&1 && [[ "$(getenforce 2>/dev/null || true)" == "Enforcing" ]]; then
  echo "  [WARN] SELinux is Enforcing. If strange denials appear: ausearch -m avc -ts recent"
fi
if [[ "${FWD_HINT}" -eq 1 ]]; then
  echo "  ^^^ 局域网访问报 ERR_CONNECTION_TIMED_OUT 基本都出在上面这些防火墙规则；"
  echo "      本机 self-check 为 [OK] 但浏览器打不开 -> 执行上面的放行命令后重试。"
fi

echo "[5/6] Preparing network & starting backend..."
if ! docker network inspect "${NETWORK_NAME}" >/dev/null 2>&1; then
  docker network create "${NETWORK_NAME}"
fi

# 可选：不改镜像、只改运行期配置时用这些环境变量覆盖 application-<env>.properties
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

echo "[6/6] Starting frontend (nginx listens inside on ${FRONTEND_CONTAINER_PORT}; host publish on ${FRONTEND_PORT}; upstream ${BACKEND_NAME})..."
docker run -d \
  --name "${FRONTEND_NAME}" \
  --network "${NETWORK_NAME}" \
  --restart unless-stopped \
  -p "0.0.0.0:${FRONTEND_PORT}:${FRONTEND_CONTAINER_PORT}" \
  -e "BACKEND_HOST=${BACKEND_NAME}" \
  "${REMOTE_FRONTEND}"

echo
echo "[SELF-CHECK] Waiting 6s for containers to come up, then probing from THIS server (bypasses LAN firewall) ..."
sleep 6
if curl -fsS --max-time 5 -o /dev/null "http://127.0.0.1:${FRONTEND_PORT}/"; then
  echo "  [OK] ${STACK_ENV} frontend reachable from THIS server (container nginx + docker-publish working)."
  echo "       If a browser cannot open it, the cause is outside Docker:"
  echo "       firewalld/ufw (see warnings above), vm/cloud security group, or wrong IP."
else
  echo "  [FAIL] ${STACK_ENV} frontend unreachable even from server loopback. Likely causes:"
  echo "       1) container exited:      docker ps -a | grep ${FRONTEND_NAME}"
  echo "       2) nginx config error:    docker logs ${FRONTEND_NAME}"
  echo "       3) wrong upstream/port:   docker exec ${FRONTEND_NAME} nginx -T | grep -nE '^[[:space:]]*listen|proxy_pass'"
fi
# 用受保护接口探测：能拿到任意 HTTP 状态码（401 也算）就说明后端已起来。
# 不用 /doc.html，因为项目未开启该接口，会误报“未就绪”。
# Spring 冷启动约 8-15s，这里最多轮询 45s。
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
  echo "  [OK] ${STACK_ENV} backend is serving on port ${BACKEND_PORT} (HTTP ${BACKEND_CODE} from /api/auth/me)."
else
  echo "  [WARN] ${STACK_ENV} backend not reachable on port ${BACKEND_PORT} after ~45s, or crashed:"
  echo "         docker logs -f ${BACKEND_NAME}"
  echo "         注意：${SPRING_PROFILE} profile 连接的 MySQL/MinIO 若不可达，后端起不来。"
  echo "         可用 DB_URL / MINIO_ENDPOINT 覆盖，例如："
  echo "         DB_URL='jdbc:mysql://<host>:3306/<db>?...' ./deploy-${STACK_ENV}-from-harbor.sh"
fi

echo
echo "============================================================"
echo "[OK] ${STACK_ENV} deploy finished."
echo "    Frontend (${STACK_ENV}):  http://${SERVER_IP}:${FRONTEND_PORT}"
echo "    Backend  (${STACK_ENV}):  http://${SERVER_IP}:${BACKEND_PORT}"
echo
echo "其他环境地址（本次未改动）:"
echo "    prd:  http://${SERVER_IP}:8091  /  http://${SERVER_IP}:12300"
echo "    uat:  http://${SERVER_IP}:8092  /  http://${SERVER_IP}:12301"
echo "    dev:  http://${SERVER_IP}:8093  /  http://${SERVER_IP}:12302"
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
