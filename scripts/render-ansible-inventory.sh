#!/usr/bin/env bash
# Формирует Ansible inventory из JSON-описания хоста (см. resolve_ssh_host.py).
# Источник JSON: первый аргумент или stdin. Формат: {"host":"...","user":"...","port":"..."}.
set -euo pipefail

input="${1:--}"
ssh_key_file="${SSH_KEY_FILE:-$HOME/.ssh/debtsapp_vm}"

if [[ "$input" == "-" ]]; then
  payload="$(cat)"
else
  payload="$(cat "$input")"
fi

host="$(printf '%s' "$payload" | python3 -c 'import json,sys;print(json.load(sys.stdin).get("host",""))')"
user="$(printf '%s' "$payload" | python3 -c 'import json,sys;print(json.load(sys.stdin).get("user",""))')"
port="$(printf '%s' "$payload" | python3 -c 'import json,sys;print(json.load(sys.stdin).get("port",""))')"

if [[ -z "$host" || -z "$user" || -z "$port" ]]; then
  echo "Invalid host descriptor: host='$host' user='$user' port='$port'" >&2
  exit 1
fi

cat <<EOF
[debtsapp]
${host} ansible_user=${user} ansible_port=${port} ansible_ssh_private_key_file=${ssh_key_file}
EOF
