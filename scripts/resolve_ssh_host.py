#!/usr/bin/env python3
# Определяет параметры SSH для целевой VM: сначала через terraform output,
# затем через REST API Yandex Cloud (с обменом OAuth-токена на IAM-токен).
# Печатает JSON вида {"host": "...", "user": "...", "port": "..."} в stdout.
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

IAM_TOKENS_URL = "https://iam.api.cloud.yandex.net/iam/v1/tokens"
COMPUTE_INSTANCES_URL = "https://compute.api.cloud.yandex.net/compute/v1/instances"


def terraform_output(tf_dir: str, name: str) -> str:
    try:
        result = subprocess.run(
            ["terraform", f"-chdir={tf_dir}", "output", "-raw", name],
            check=False,
            capture_output=True,
            text=True,
        )
    except FileNotFoundError:
        return ""
    if result.returncode != 0:
        return ""
    return result.stdout.strip()


def _request_with_retry(req: urllib.request.Request, *, retries: int = 4, timeout: int = 30) -> bytes:
    delay = 5
    last_exc: Exception = RuntimeError("no attempts made")
    for attempt in range(retries):
        try:
            with urllib.request.urlopen(req, timeout=timeout) as response:
                return response.read()
        except urllib.error.HTTPError as exc:
            if exc.code < 500:
                raise
            last_exc = exc
        except OSError as exc:
            last_exc = exc
        if attempt < retries - 1:
            print(f"YC API error ({last_exc}), retrying in {delay}s…", file=sys.stderr)
            time.sleep(delay)
            delay = min(delay * 2, 60)
    raise last_exc


def exchange_oauth_for_iam(oauth_token: str) -> str:
    payload = json.dumps({"yandexPassportOauthToken": oauth_token}).encode("utf-8")
    req = urllib.request.Request(
        IAM_TOKENS_URL,
        data=payload,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    body = json.loads(_request_with_retry(req).decode("utf-8"))
    token = body.get("iamToken")
    if not token:
        raise RuntimeError("IAM token exchange returned empty iamToken")
    return token


def fetch_instances(iam_token: str, folder_id: str) -> dict:
    url = f"{COMPUTE_INSTANCES_URL}?folderId={folder_id}"
    req = urllib.request.Request(url, headers={"Authorization": f"Bearer {iam_token}"})
    return json.loads(_request_with_retry(req).decode("utf-8"))


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


def resolve_host(tf_dir: str, vm_name: str) -> str:
    host = terraform_output(tf_dir, "public_ip")
    if host:
        return host

    # Допускаем подсказку результата запроса к API через файл для e2e-тестов.
    cached_path = os.environ.get("YC_INSTANCES_JSON")
    if cached_path and Path(cached_path).exists():
        with Path(cached_path).open(encoding="utf-8") as fh:
            return extract_public_ip(json.load(fh), vm_name)

    folder_id = os.environ.get("YC_FOLDER_ID") or os.environ.get("FOLDER_ID", "")
    oauth_token = os.environ.get("YC_TOKEN", "")
    if not folder_id:
        raise RuntimeError("YC_FOLDER_ID is not set")
    if not oauth_token:
        raise RuntimeError("YC_TOKEN is not set")
    iam_token = exchange_oauth_for_iam(oauth_token)
    instances = fetch_instances(iam_token, folder_id)
    return extract_public_ip(instances, vm_name)


def main() -> int:
    vm_name = os.environ.get("VM_NAME", "")
    if not vm_name:
        print("VM_NAME is not set", file=sys.stderr)
        return 1

    tf_dir = os.environ.get("TF_DIR", "infra/terraform/yandex")

    try:
        host = resolve_host(tf_dir, vm_name)
    except Exception as exc:  # noqa: BLE001
        print(f"Failed to resolve SSH host: {exc}", file=sys.stderr)
        return 1

    if not host:
        print(f"Failed to resolve SSH host for {vm_name}", file=sys.stderr)
        return 1

    user = terraform_output(tf_dir, "ssh_user") or os.environ.get("SSH_USER_FALLBACK", "macsia")
    port = terraform_output(tf_dir, "ssh_port") or os.environ.get("SSH_PORT_FALLBACK", "22")

    json.dump({"host": host, "user": user, "port": port}, sys.stdout)
    sys.stdout.write("\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
