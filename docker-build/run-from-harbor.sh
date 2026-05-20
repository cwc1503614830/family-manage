#!/usr/bin/env bash
# Pull family-manage images from Harbor (HTTP) and run backend + frontend.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Defaults (override via docker-build/.env)
HARBOR_REGISTRY="${HARBOR_REGISTRY:-192.168.216.140:8091}"
HARBOR_PROJECT="${HARBOR_PROJECT:-family-manage}"
HARBOR_USER="${HARBOR_USER:-admin}"
HARBOR_PASSWORD="${HARBOR_PASSWORD:-Harbor12345}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
BACKEND_REPO="${BACKEND_REPO:-family-manage-backend}"
FRONTEND_REPO="${FRONTEND_REPO:-family-manage-frontend}"
NETWORK_NAME="${NETWORK_NAME:-family-manage-net}"
BACKEND_CONTAINER="${BACKEND_CONTAINER:-family-manage-backend}"
FRONTEND_CONTAINER="${FRONTEND_CONTAINER:-family-manage-frontend}"
BACKEND_HOST_PORT="${BACKEND_HOST_PORT:-12300}"
FRONTEND_HOST_PORT="${FRONTEND_HOST_PORT:-8080}"

# Parse .env safely (supports Windows CRLF line endings)
load_env() {
  local env_file="$SCRIPT_DIR/.env"
  [[ -f "$env_file" ]] || return 0

  while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line//$'\r'/}"
    [[ -z "$line" || "$line" =~ ^[[:space:]]*# ]] && continue
    if [[ "$line" =~ ^([A-Za-z_][A-Za-z0-9_]*)=(.*)$ ]]; then
      local key="${BASH_REMATCH[1]}"
      local val="${BASH_REMATCH[2]}"
      val="${val%\"}"
      val="${val#\"}"
      val="${val%\'}"
      val="${val#\'}"
      val="${val#"${val%%[![:space:]]*}"}"
      val="${val%"${val##*[![:space:]]}"}"
      export "${key}=${val}"
    fi
  done < "$env_file"
}

normalize_registry() {
  HARBOR_REGISTRY="${HARBOR_REGISTRY#https://}"
  HARBOR_REGISTRY="${HARBOR_REGISTRY#http://}"
  HARBOR_REGISTRY="${HARBOR_REGISTRY%/}"
  HARBOR_REGISTRY="${HARBOR_REGISTRY//$'\r'/}"
}

usage() {
  cat <<'EOF'
Usage: ./run-from-harbor.sh [command]
       bash run-from-harbor.sh [command]

Commands:
  up       Pull images, create network, start containers (default)
  stop     Stop and remove containers
  restart  stop then up
  status   Show container status
  logs     Follow logs (backend + frontend)
  help     Show this help

Configure via docker-build/.env (copy from .env.example).

If copied from Windows and ./run-from-harbor.sh fails, run once:
  sed -i 's/\r$//' run-from-harbor.sh .env
  chmod +x run-from-harbor.sh

Harbor uses HTTP. Linux must configure insecure-registries first:
  sudo ./setup-docker-harbor-linux.sh 192.168.216.140:8091
  ./run-from-harbor.sh

EOF
}

require_harbor_insecure_registry() {
  if docker info 2>/dev/null | grep -Fq "${HARBOR_REGISTRY}"; then
    return 0
  fi

  echo "[ERROR] Docker is using HTTPS for Harbor, but Harbor is HTTP only." >&2
  echo "        (${HARBOR_REGISTRY})" >&2
  echo "" >&2
  echo "Fix on Linux (run once as root):" >&2
  echo "  cd ${SCRIPT_DIR}" >&2
  echo "  sed -i 's/\\r$//' setup-docker-harbor-linux.sh" >&2
  echo "  chmod +x setup-docker-harbor-linux.sh" >&2
  echo "  sudo ./setup-docker-harbor-linux.sh ${HARBOR_REGISTRY}" >&2
  echo "" >&2
  echo "Or edit /etc/docker/daemon.json manually:" >&2
  echo '  { "insecure-registries": ["'${HARBOR_REGISTRY}'"] }' >&2
  echo "  sudo systemctl restart docker" >&2
  echo "" >&2
  echo "Verify: docker info | grep -A3 'Insecure Registries'" >&2
  exit 1
}

require_docker() {
  if ! command -v docker >/dev/null 2>&1; then
    echo "[ERROR] docker not found in PATH." >&2
    exit 1
  fi
}

harbor_configured_in_docker() {
  local cfg="${DOCKER_CONFIG:-$HOME/.docker}/config.json"
  [[ -f "$cfg" ]] && grep -Fq "${HARBOR_REGISTRY}" "$cfg" 2>/dev/null
}

harbor_password_is_placeholder() {
  [[ -z "${HARBOR_PASSWORD}" || "${HARBOR_PASSWORD}" == "your-password-here" ]]
}

harbor_login() {
  echo "[1/4] Logging in to Harbor (HTTP): ${HARBOR_REGISTRY}"

  if [[ "${SKIP_HARBOR_LOGIN:-0}" == "1" ]]; then
    echo "       SKIP_HARBOR_LOGIN=1, using existing docker credentials."
    return 0
  fi

  if harbor_configured_in_docker() && [[ "${FORCE_HARBOR_LOGIN:-0}" != "1" ]]; then
    echo "       Already logged in to ${HARBOR_REGISTRY}, skip login."
    echo "       (manual: docker login ${HARBOR_REGISTRY} -u ${HARBOR_USER})"
    return 0
  fi

  if harbor_password_is_placeholder; then
    echo "[ERROR] HARBOR_PASSWORD not set in .env, and not logged in yet." >&2
    echo "        Fix one of:" >&2
    echo "          1) echo 'HARBOR_PASSWORD=你的密码' >> .env" >&2
    echo "          2) docker login ${HARBOR_REGISTRY} -u ${HARBOR_USER}" >&2
    echo "             SKIP_HARBOR_LOGIN=1 ./run-from-harbor.sh" >&2
    exit 1
  fi

  if ! printf '%s' "${HARBOR_PASSWORD}" | docker login "${HARBOR_REGISTRY}" -u "${HARBOR_USER}" --password-stdin; then
    echo "" >&2
    echo "[ERROR] Harbor login failed (unauthorized)." >&2
    echo "        Manual login works? Then .env password is wrong." >&2
    echo "        Edit .env: HARBOR_PASSWORD=<same password as manual login>" >&2
    echo "        Or after manual login run:" >&2
    echo "          SKIP_HARBOR_LOGIN=1 ./run-from-harbor.sh" >&2
    exit 1
  fi
  echo "       Login Succeeded."
}

pull_images() {
  local backend_image frontend_image
  backend_image="${HARBOR_REGISTRY}/${HARBOR_PROJECT}/${BACKEND_REPO}:${IMAGE_TAG}"
  frontend_image="${HARBOR_REGISTRY}/${HARBOR_PROJECT}/${FRONTEND_REPO}:${IMAGE_TAG}"

  echo "[2/4] Pulling images..."
  docker pull "${backend_image}"
  docker pull "${frontend_image}"

  BACKEND_IMAGE="${backend_image}"
  FRONTEND_IMAGE="${frontend_image}"
}

ensure_network() {
  if ! docker network inspect "${NETWORK_NAME}" >/dev/null 2>&1; then
    echo "[3/4] Creating network: ${NETWORK_NAME}"
    docker network create "${NETWORK_NAME}"
  else
    echo "[3/4] Using existing network: ${NETWORK_NAME}"
  fi
}

stop_containers() {
  echo "Stopping containers..."
  docker rm -f "${BACKEND_CONTAINER}" "${FRONTEND_CONTAINER}" 2>/dev/null || true
}

run_containers() {
  echo "[4/4] Starting containers..."

  docker run -d \
    --name "${BACKEND_CONTAINER}" \
    --network "${NETWORK_NAME}" \
    --restart unless-stopped \
    -p "${BACKEND_HOST_PORT}:12300" \
    "${BACKEND_IMAGE}"

  docker run -d \
    --name "${FRONTEND_CONTAINER}" \
    --network "${NETWORK_NAME}" \
    --restart unless-stopped \
    -p "${FRONTEND_HOST_PORT}:80" \
    "${FRONTEND_IMAGE}"

  echo ""
  echo "[OK] Services are running."
  echo "  Frontend: http://<host>:${FRONTEND_HOST_PORT}/"
  echo "  Backend:  http://<host>:${BACKEND_HOST_PORT}/"
  echo "  API via nginx: http://<host>:${FRONTEND_HOST_PORT}/api/"
  echo ""
  docker ps -a --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}' \
    | grep -E "NAMES|${BACKEND_CONTAINER}|${FRONTEND_CONTAINER}" || true
}

cmd_up() {
  require_docker
  load_env
  normalize_registry

  echo "============================================================"
  echo "Harbor registry : ${HARBOR_REGISTRY} (HTTP)"
  echo "Project         : ${HARBOR_PROJECT}"
  echo "Tag             : ${IMAGE_TAG}"
  echo "Backend image   : ${HARBOR_REGISTRY}/${HARBOR_PROJECT}/${BACKEND_REPO}:${IMAGE_TAG}"
  echo "Frontend image  : ${HARBOR_REGISTRY}/${HARBOR_PROJECT}/${FRONTEND_REPO}:${IMAGE_TAG}"
  echo "============================================================"
  echo ""

  require_harbor_insecure_registry

  stop_containers
  harbor_login
  pull_images
  ensure_network
  run_containers
}

cmd_stop() {
  require_docker
  load_env
  stop_containers
  echo "[OK] Containers removed."
}

cmd_restart() {
  cmd_stop
  cmd_up
}

cmd_status() {
  require_docker
  load_env
  docker ps -a --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}' \
    | grep -E "NAMES|${BACKEND_CONTAINER}|${FRONTEND_CONTAINER}" || true
}

cmd_logs() {
  require_docker
  load_env
  docker logs -f "${BACKEND_CONTAINER}" &
  docker logs -f "${FRONTEND_CONTAINER}" &
  wait
}

main() {
  local cmd="${1:-up}"
  case "${cmd}" in
    up) cmd_up ;;
    stop) cmd_stop ;;
    restart) cmd_restart ;;
    status) cmd_status ;;
    logs) cmd_logs ;;
    help|-h|--help) usage ;;
    *)
      echo "[ERROR] Unknown command: ${cmd}" >&2
      usage
      exit 1
      ;;
  esac
}

main "$@"
