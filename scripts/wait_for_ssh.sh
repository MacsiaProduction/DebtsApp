#!/usr/bin/env bash
# Ожидает доступности SSH-порта и добавляет host-key в known_hosts.
set -euo pipefail

HOST="${1:?host is required}"
PORT="${2:-22}"
RETRIES="${SSH_WAIT_RETRIES:-30}"
DELAY="${SSH_WAIT_DELAY:-5}"
KNOWN_HOSTS="${KNOWN_HOSTS_FILE:-$HOME/.ssh/known_hosts}"

mkdir -p "$(dirname "$KNOWN_HOSTS")"
touch "$KNOWN_HOSTS"
chmod 644 "$KNOWN_HOSTS"

for _ in $(seq 1 "$RETRIES"); do
  if timeout 5 bash -c "cat < /dev/null > /dev/tcp/${HOST}/${PORT}" 2>/dev/null; then
    if ssh-keyscan -p "$PORT" -H "$HOST" >> "$KNOWN_HOSTS" 2>/dev/null; then
      exit 0
    fi
  fi
  sleep "$DELAY"
done

echo "Timed out waiting for SSH on ${HOST}:${PORT}" >&2
exit 1
