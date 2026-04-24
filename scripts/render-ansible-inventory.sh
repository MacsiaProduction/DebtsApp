#!/usr/bin/env bash
set -euo pipefail

terraform_dir="${1:-infra/terraform/yandex}"
ssh_key_file="${SSH_KEY_FILE:-$HOME/.ssh/debtsapp_vm}"

host="$(terraform -chdir="$terraform_dir" output -raw public_ip)"
user="$(terraform -chdir="$terraform_dir" output -raw ssh_user)"
port="$(terraform -chdir="$terraform_dir" output -raw ssh_port)"

cat <<EOF
[debtsapp]
${host} ansible_user=${user} ansible_port=${port} ansible_ssh_private_key_file=${ssh_key_file}
EOF
