#!/usr/bin/env bash
# Применяет terraform-конфигурацию и считает ошибку AlreadyExists некритичной.
set -euo pipefail

TF_DIR="${1:-infra/terraform/yandex}"
LOG_FILE="${TF_APPLY_LOG:-/tmp/terraform-apply.log}"

set +e
terraform -chdir="$TF_DIR" apply -auto-approve 2>&1 | tee "$LOG_FILE"
status=${PIPESTATUS[0]}
set -e

if [ "$status" -eq 0 ]; then
  exit 0
fi

if grep -q 'AlreadyExists desc = Instance with name' "$LOG_FILE"; then
  echo "VM already exists, continuing with existing infrastructure."
  exit 0
fi

exit "$status"
