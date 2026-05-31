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
FRONTEND_PORT="${FRONTEND_PORT:-8080}"
SPRING_PROFILE="${SPRING_PROFILE:-dev}"

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

if ! command -v docker >/dev/null 2>&1; then
  echo "[ERROR] docker not found."
  exit 1
fi

echo "[1/5] Logging in to Harbor..."
echo "${HARBOR_PASS}" | docker login "${HARBOR_HOST}" -u "${HARBOR_USER}" --password-stdin

echo "[2/5] Pulling images..."
docker pull "${REMOTE_BACKEND}"
docker pull "${REMOTE_FRONTEND}"

echo "[3/5] Preparing network..."
if ! docker network inspect "${NETWORK_NAME}" >/dev/null 2>&1; then
  docker network create "${NETWORK_NAME}"
fi

echo "[4/5] Starting backend..."
docker rm -f "${BACKEND_NAME}" >/dev/null 2>&1 || true
docker run -d \
  --name "${BACKEND_NAME}" \
  --network "${NETWORK_NAME}" \
  --restart unless-stopped \
  -p "${BACKEND_PORT}:12300" \
  -e "SPRING_PROFILES_ACTIVE=${SPRING_PROFILE}" \
  "${REMOTE_BACKEND}"

echo "[5/5] Starting frontend..."
docker rm -f "${FRONTEND_NAME}" >/dev/null 2>&1 || true
docker run -d \
  --name "${FRONTEND_NAME}" \
  --network "${NETWORK_NAME}" \
  --restart unless-stopped \
  -p "${FRONTEND_PORT}:80" \
  "${REMOTE_FRONTEND}"

echo
echo "[OK] Services started."
echo "  Frontend: http://<server-ip>:${FRONTEND_PORT}"
echo "  Backend:  http://<server-ip>:${BACKEND_PORT}"
echo
docker ps --filter "name=${BACKEND_NAME}" --filter "name=${FRONTEND_NAME}" \
  --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
