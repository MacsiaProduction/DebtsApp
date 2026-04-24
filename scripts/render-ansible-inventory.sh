#!/usr/bin/env bash
set -euo pipefail

terraform_dir="${1:-infra/terraform/yandex}"
ssh_key_file="${SSH_KEY_FILE:-$HOME/.ssh/debtsapp_vm}"
fallback_host="${APP_HOST:-}"
fallback_user="${SSH_USER_FALLBACK:-macsia}"
fallback_port="${SSH_PORT_FALLBACK:-22}"

host="$(terraform -chdir="$terraform_dir" output -raw public_ip 2>/dev/null || true)"
user="$(terraform -chdir="$terraform_dir" output -raw ssh_user 2>/dev/null || true)"
port="$(terraform -chdir="$terraform_dir" output -raw ssh_port 2>/dev/null || true)"

host="${host:-$fallback_host}"
user="${user:-$fallback_user}"
port="${port:-$fallback_port}"

if [[ -z "$host" || -z "$user" || -z "$port" ]]; then
  echo "Failed to resolve inventory host/user/port from Terraform outputs or fallback env" >&2
  exit 1
fi

cat <<EOF
[debtsapp]
${host} ansible_user=${user} ansible_port=${port} ansible_ssh_private_key_file=${ssh_key_file}
EOF
