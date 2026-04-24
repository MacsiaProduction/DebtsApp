#!/usr/bin/env python3
# Формирует terraform.auto.tfvars.json из переменных окружения.
import json
import os
from pathlib import Path


def main() -> int:
    data = {
        "cloud_id": os.environ["CLOUD_ID"],
        "folder_id": os.environ["FOLDER_ID"],
        "zone": os.environ["ZONE"],
        "vm_name": os.environ["VM_NAME"],
        "ssh_user": os.environ.get("SSH_USER", "macsia"),
        "ssh_public_key": os.environ["SSH_PUBLIC_KEY"],
        "admin_password_hash": os.environ["ADMIN_PASSWORD_HASH"],
        "vm_cores": int(os.environ.get("VM_CORES", "2")),
        "vm_memory_gb": int(os.environ.get("VM_MEMORY_GB", "4")),
        "vm_core_fraction": int(os.environ.get("VM_CORE_FRACTION", "50")),
        "boot_disk_size_gb": int(os.environ.get("BOOT_DISK_SIZE_GB", "50")),
        "boot_disk_type": os.environ.get("BOOT_DISK_TYPE", "network-hdd"),
        "app_domain": os.environ["APP_DOMAIN"],
        "acme_email": os.environ["ACME_EMAIL"],
    }
    output_path = Path(
        os.environ.get(
            "TF_VARS_OUTPUT",
            "infra/terraform/yandex/terraform.auto.tfvars.json",
        )
    )
    output_path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
