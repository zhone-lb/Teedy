#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC="${SCRIPT_DIR}/docker-daemon.json"
DEST="/etc/docker/daemon.json"

if [[ ! -f "$SRC" ]]; then
  echo "Missing ${SRC}" >&2
  exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is not installed." >&2
  exit 1
fi

sudo mkdir -p /etc/docker

if [[ -f "$DEST" ]]; then
  backup="${DEST}.bak.$(date +%Y%m%d%H%M%S)"
  echo "Backing up existing ${DEST} -> ${backup}"
  sudo cp "$DEST" "$backup"
fi

echo "Installing registry mirrors to ${DEST}"
sudo cp "$SRC" "$DEST"
sudo chmod 644 "$DEST"

echo "Restarting Docker..."
if systemctl is-active --quiet docker 2>/dev/null; then
  sudo systemctl restart docker
elif command -v service >/dev/null 2>&1; then
  sudo service docker restart
else
  echo "Could not restart Docker automatically. Run: sudo systemctl restart docker" >&2
  exit 1
fi

echo ""
echo "Registry mirrors (from docker info):"
docker info 2>/dev/null | sed -n '/Registry Mirrors/,/^[^ ]/p' | head -10

echo ""
echo "Test pull: docker pull ubuntu:22.04"
