#!/usr/bin/env bash
# Configure Docker on Linux to use HTTP Harbor (insecure-registries).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REGISTRY="${1:-192.168.216.140:8091}"
REGISTRY="${REGISTRY#https://}"
REGISTRY="${REGISTRY#http://}"
REGISTRY="${REGISTRY%/}"

DAEMON_JSON="/etc/docker/daemon.json"

if [[ "${EUID}" -ne 0 ]]; then
  echo "[ERROR] Please run as root: sudo $0 [registry-host:port]"
  exit 1
fi

if ! command -v python3 >/dev/null 2>&1; then
  echo "[ERROR] python3 is required to merge daemon.json safely."
  exit 1
fi

mkdir -p /etc/docker
if [[ -f "${DAEMON_JSON}" ]]; then
  cp "${DAEMON_JSON}" "${DAEMON_JSON}.bak.$(date +%Y%m%d%H%M%S)"
  echo "Backed up existing ${DAEMON_JSON}"
fi

REGISTRY="${REGISTRY}" DAEMON_JSON="${DAEMON_JSON}" python3 <<'PY'
import json, os, sys

registry = os.environ["REGISTRY"]
path = os.environ["DAEMON_JSON"]
data = {}
if os.path.isfile(path):
    with open(path, encoding="utf-8") as f:
        content = f.read().strip()
        if content:
            data = json.loads(content)

regs = data.get("insecure-registries") or []
if not isinstance(regs, list):
    regs = [regs]
if registry not in regs:
    regs.append(registry)
data["insecure-registries"] = regs

with open(path, "w", encoding="utf-8") as f:
    json.dump(data, f, indent=2)
    f.write("\n")

print("Updated", path)
print(json.dumps(data, indent=2))
PY

echo ""
echo "Restarting Docker..."
if command -v systemctl >/dev/null 2>&1; then
  systemctl restart docker
else
  service docker restart
fi

sleep 2
echo ""
echo "Insecure registries:"
docker info 2>/dev/null | sed -n '/Insecure Registries/,/^[^ ]/p' | head -20

if docker info 2>/dev/null | grep -Fq "${REGISTRY}"; then
  echo ""
  echo "[OK] ${REGISTRY} is configured. You can run: ./run-from-harbor.sh"
else
  echo ""
  echo "[WARN] ${REGISTRY} not visible in docker info yet. Check ${DAEMON_JSON} and restart docker."
fi
