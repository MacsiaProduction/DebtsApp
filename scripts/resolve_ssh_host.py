#!/usr/bin/env python3
# Определяет публичный IP целевой VM: сначала через terraform output,
# затем через REST API Yandex Cloud (с обменом OAuth-токена на IAM-токен).
import json
import os
import subprocess
import sys
import urllib.request
from pathlib import Path

IAM_TOKENS_URL = "https://iam.api.cloud.yandex.net/iam/v1/tokens"
COMPUTE_INSTANCES_URL = "https://compute.api.cloud.yandex.net/compute/v1/instances"


def terraform_public_ip(tf_dir: str) -> str:
    try:
        result = subprocess.run(
            ["terraform", f"-chdir={tf_dir}", "output", "-raw", "public_ip"],
            check=False,
            capture_output=True,
            text=True,
        )
    except FileNotFoundError:
        return ""
    if result.returncode != 0:
        return ""
    return result.stdout.strip()


def exchange_oauth_for_iam(oauth_token: str) -> str:
    payload = json.dumps({"yandexPassportOauthToken": oauth_token}).encode("utf-8")
    request = urllib.request.Request(
        IAM_TOKENS_URL,
        data=payload,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=30) as response:
        body = json.loads(response.read().decode("utf-8"))
    token = body.get("iamToken")
    if not token:
        raise RuntimeError("IAM token exchange returned empty iamToken")
    return token


def fetch_instances(iam_token: str, folder_id: str) -> dict:
    url = f"{COMPUTE_INSTANCES_URL}?folderId={folder_id}"
    request = urllib.request.Request(
        url,
        headers={"Authorization": f"Bearer {iam_token}"},
    )
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.loads(response.read().decode("utf-8"))


def extract_public_ip(instances: dict, vm_name: str) -> str:
    for instance in instances.get("instances", []):
        if instance.get("name") != vm_name:
            continue
        for network_interface in instance.get("networkInterfaces", []):
            primary_v4 = network_interface.get("primaryV4Address") or {}
            nat = primary_v4.get("oneToOneNat") or {}
            address = nat.get("address")
            if address:
                return address
    return ""


def main() -> int:
    vm_name = os.environ.get("VM_NAME", "")
    if not vm_name:
        print("VM_NAME is not set", file=sys.stderr)
        return 1

    tf_dir = os.environ.get("TF_DIR", "infra/terraform/yandex")

    host = terraform_public_ip(tf_dir)

    # Допускаем подсказку результата запроса к API через файл для e2e-тестов.
    cached_path = os.environ.get("YC_INSTANCES_JSON")
    if not host and cached_path and Path(cached_path).exists():
        with Path(cached_path).open(encoding="utf-8") as fh:
            instances = json.load(fh)
        host = extract_public_ip(instances, vm_name)

    if not host:
        folder_id = os.environ.get("YC_FOLDER_ID") or os.environ.get("FOLDER_ID", "")
        oauth_token = os.environ.get("YC_TOKEN", "")
        if not folder_id:
            print("YC_FOLDER_ID is not set", file=sys.stderr)
            return 1
        if not oauth_token:
            print("YC_TOKEN is not set", file=sys.stderr)
            return 1
        iam_token = exchange_oauth_for_iam(oauth_token)
        instances = fetch_instances(iam_token, folder_id)
        host = extract_public_ip(instances, vm_name)

    if not host:
        print(f"Failed to resolve SSH host for {vm_name}", file=sys.stderr)
        return 1

    print(host)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
