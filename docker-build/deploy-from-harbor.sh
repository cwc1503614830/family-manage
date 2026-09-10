#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  ./deploy-from-harbor.sh [tag] [harbor-project]

Defaults:
  tag            latest
  harbor-project family-manage

Environment overrides:
  HARBOR_HOST, HARBOR_USER, HARBOR_PASS
  BACKEND_PORT, FRONTEND_PORT, SPRING_PROFILE

Example:
  ./deploy-from-harbor.sh v1.0.0 family-manage
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

NETWORK_NAME="${NETWORK_NAME:-family-manage-net}"
BACKEND_NAME="${BACKEND_NAME:-family-manage-backend}"
FRONTEND_NAME="${FRONTEND_NAME:-family-manage-frontend}"
BACKEND_PORT="${BACKEND_PORT:-12300}"
# NOTE: Container nginx listens on 80 (see nginx.conf `listen 80`).
#       FRONTEND_PORT below is the HOST-facing port.
#       Container nginx default is 80 so FRONTEND_CONTAINER_PORT=80.
FRONTEND_PORT="${FRONTEND_PORT:-8080}"
FRONTEND_CONTAINER_PORT=80
SPRING_PROFILE="${SPRING_PROFILE:-prd}"

REMOTE_BACKEND="${HARBOR_HOST}/${HARBOR_PROJECT}/family-manage-backend:${TAG}"
REMOTE_FRONTEND="${HARBOR_HOST}/${HARBOR_PROJECT}/family-manage-frontend:${TAG}"

echo
echo "============================================================"
echo "Deploy Family Manage from Harbor"
echo "Registry:  ${HARBOR_HOST}"
echo "Project:   ${HARBOR_PROJECT}"
echo "Tag:       ${TAG}"
echo "Backend:   ${REMOTE_BACKEND}"
echo "Frontend:  ${REMOTE_FRONTEND}"
echo "============================================================"
echo
echo "NOTE: HTTP Harbor requires insecure-registries on this host:"
echo '  /etc/docker/daemon.json -> "insecure-registries": ["192.168.236.129:80"]'
echo "  then: sudo systemctl restart docker"
echo
# Detect primary IPv4 of this server so the final URL block is clickable and concrete.
SERVER_IP="$(hostname -I 2>/dev/null | awk '{print $1}')"
if [[ -z "${SERVER_IP}" ]]; then
  SERVER_IP="$(ip -4 route get 1 2>/dev/null | awk '{print $7; exit}')"
fi
if [[ -z "${SERVER_IP}" ]]; then SERVER_IP="<server-ip>"; fi
echo "Detected server IPv4: ${SERVER_IP}"
echo "  Expected URLs after successful deploy:"
echo "    Frontend: http://${SERVER_IP}:${FRONTEND_PORT}"
echo "    Backend:  http://${SERVER_IP}:${BACKEND_PORT}"
echo

if ! command -v docker >/dev/null 2>&1; then
  echo "[ERROR] docker not found."
  exit 1
fi

echo "[1/5] Logging in to Harbor..."
echo "${HARBOR_PASS}" | docker login "${HARBOR_HOST}" -u "${HARBOR_USER}" --password-stdin

echo "[2/5] Pulling images..."
docker pull "${REMOTE_BACKEND}"
docker pull "${REMOTE_FRONTEND}"

echo "[3/6] Host firewall / security precheck ..."
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
  echo "         Usually Docker SELinux policies are fine; focus on firewall if unsure."
fi
if [[ "${FWD_HINT}" -eq 1 ]]; then
  echo "  ^^^ 95% of 'ERR_CONNECTION_TIMED_OUT' from LAN PCs are caused by the firewall rules above."
  echo "      If browser timeout occurs but server self-check (below) shows [OK] -> run those cmds and refresh."
fi

echo "[4/6] Preparing network..."
if ! docker network inspect "${NETWORK_NAME}" >/dev/null 2>&1; then
  docker network create "${NETWORK_NAME}"
fi

echo "[5/6] Starting backend..."
docker rm -f "${BACKEND_NAME}" >/dev/null 2>&1 || true
docker run -d \
  --name "${BACKEND_NAME}" \
  --network "${NETWORK_NAME}" \
  --restart unless-stopped \
  -p "0.0.0.0:${BACKEND_PORT}:12300" \
  -e "SPRING_PROFILES_ACTIVE=${SPRING_PROFILE}" \
  "${REMOTE_BACKEND}"

echo "[6/6] Starting frontend (nginx listens inside on ${FRONTEND_CONTAINER_PORT}; host publish on ${FRONTEND_PORT})..."
docker rm -f "${FRONTEND_NAME}" >/dev/null 2>&1 || true
docker run -d \
  --name "${FRONTEND_NAME}" \
  --network "${NETWORK_NAME}" \
  --restart unless-stopped \
  -p "0.0.0.0:${FRONTEND_PORT}:${FRONTEND_CONTAINER_PORT}" \
  "${REMOTE_FRONTEND}"

echo
echo "[SELF-CHECK] Waiting 6s for containers to come up, then probing from THIS server (bypasses LAN firewall) ..."
sleep 6
FE_OK=0
if curl -fsS --max-time 5 -o /dev/null "http://127.0.0.1:${FRONTEND_PORT}/"; then
  echo "  [OK] Frontend reachable from THIS server (container nginx + docker-publish working correctly)."
  echo "       If Windows PC browser shows ERR_CONNECTION_TIMED_OUT/REFUSED, issue is outside Docker:"
  echo "       firewalld/ufw (see earlier warnings), cloud/vm security group, or wrong IP."
  FE_OK=1
else
  echo "  [FAIL] Frontend unreachable even from server loopback. Likely causes:"
  echo "       1) container already exited; check    docker ps -a | grep ${FRONTEND_NAME}"
  echo "       2) nginx config broken in image; see  docker logs ${FRONTEND_NAME}"
  echo "       3) image still OLD (Harbor has listen-80 version). Rebuild & push on Windows first."
fi
if curl -fsS --max-time 10 -o /dev/null "http://127.0.0.1:${BACKEND_PORT}/doc.html" 2>/dev/null; then
  echo "  [OK] Backend /doc.html reachable from this server."
else
  echo "  [WARN] Backend /doc.html not ready yet. Spring bootstraps ~15-30s, or crashed. See:"
  echo "         docker logs -f ${BACKEND_NAME}"
fi
echo
echo "============================================================"
echo "[OK] Deploy step finished. Open the following URLs FROM YOUR PC BROWSER:"
echo "    Frontend (main UI):  http://${SERVER_IP}:${FRONTEND_PORT}"
echo "    Backend API / doc:   http://${SERVER_IP}:${BACKEND_PORT}/doc.html"
echo "============================================================"
echo "If Windows browser shows errors and self-check printed [OK] for frontend:"
echo "  Step 1) Run the firewall allow commands printed earlier and refresh."
echo "  Step 2) If this VM is behind VMware/AWS/阿里云 security group, allow inbound TCP:"
echo "          ports ${FRONTEND_PORT} and ${BACKEND_PORT} (source: your LAN subnet or 0.0.0.0/0)"
echo "  Step 3) Confirm raw TCP reachability from Windows PowerShell:"
echo "          Test-NetConnection ${SERVER_IP} -Port ${FRONTEND_PORT}"
echo
docker ps --filter "name=${BACKEND_NAME}" --filter "name=${FRONTEND_NAME}" \
  --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
