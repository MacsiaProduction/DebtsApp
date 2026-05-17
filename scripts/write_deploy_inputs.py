#!/usr/bin/env python3
"""Writes terraform.auto.tfvars.json and Ansible deploy-secrets.yml from environment variables."""
import json
import os
from pathlib import Path


def yaml_quote(value: str) -> str:
    value = value or ""
    return '"' + value.replace("\\", "\\\\").replace('"', '\\"') + '"'


def write_terraform_vars() -> None:
    # Only emit keys that are explicitly set; Terraform variables.tf provides the defaults.
    required = {
        "cloud_id": "CLOUD_ID",
        "folder_id": "FOLDER_ID",
        "zone": "ZONE",
        "vm_name": "VM_NAME",
        "ssh_public_key": "SSH_PUBLIC_KEY",
        "admin_password_hash": "ADMIN_PASSWORD_HASH",
        "app_domain": "APP_DOMAIN",
        "acme_email": "ACME_EMAIL",
    }
    optional_int = {
        "vm_cores": "VM_CORES",
        "vm_memory_gb": "VM_MEMORY_GB",
        "vm_core_fraction": "VM_CORE_FRACTION",
        "boot_disk_size_gb": "BOOT_DISK_SIZE_GB",
    }
    optional_str = {
        "ssh_user": "SSH_USER",
        "boot_disk_type": "BOOT_DISK_TYPE",
    }

    data = {k: os.environ[v] for k, v in required.items()}
    data.update({k: int(os.environ[v]) for k, v in optional_int.items() if v in os.environ})
    data.update({k: os.environ[v] for k, v in optional_str.items() if v in os.environ})

    output = Path(os.environ.get("TF_VARS_OUTPUT", "infra/terraform/yandex/terraform.auto.tfvars.json"))
    output.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def write_deploy_secrets() -> None:
    lines = [
        f"app_domain: {yaml_quote(os.environ['APP_DOMAIN'])}",
        f"grafana_domain: {yaml_quote(os.environ['GRAFANA_DOMAIN'])}",
        f"acme_email: {yaml_quote(os.environ['ACME_EMAIL'])}",
        f"backend_image: {yaml_quote(os.environ['BACKEND_IMAGE'])}",
        f"frontend_image: {yaml_quote(os.environ['FRONTEND_IMAGE'])}",
        f"ghcr_username: {yaml_quote(os.environ.get('GHCR_PULL_USERNAME', ''))}",
        f"ghcr_token: {yaml_quote(os.environ.get('GHCR_PULL_TOKEN', ''))}",
        f"postgres_password: {yaml_quote(os.environ['POSTGRES_PASSWORD'])}",
        f"neo4j_password: {yaml_quote(os.environ['NEO4J_PASSWORD'])}",
        f"jwt_secret: {yaml_quote(os.environ['JWT_SECRET'])}",
        f"grafana_admin_user: {yaml_quote(os.environ.get('GRAFANA_ADMIN_USER', 'grafana'))}",
        f"grafana_admin_password: {yaml_quote(os.environ['GRAFANA_ADMIN_PASSWORD'])}",
        f"bot_token: {yaml_quote(os.environ.get('BOT_TOKEN', ''))}",
        f"argocd_domain: {yaml_quote(os.environ.get('ARGOCD_DOMAIN', 'argocd.macsia.fun'))}",
        f"k8s_dashboard_domain: {yaml_quote(os.environ.get('K8S_DASHBOARD_DOMAIN', 'dashboard.macsia.fun'))}",
        f"github_repo_url: {yaml_quote(os.environ.get('GITHUB_REPO_URL', ''))}",
        f"github_repo_token: {yaml_quote(os.environ.get('GITHUB_REPO_TOKEN', ''))}",
        f"sonarqube_domain: {yaml_quote(os.environ.get('SONARQUBE_DOMAIN', 'sonar.macsia.fun'))}",
        f"sonarqube_db_password: {yaml_quote(os.environ.get('SONARQUBE_DB_PASSWORD', ''))}",
    ]
    output = Path(os.environ.get("DEPLOY_SECRETS_OUTPUT", "infra/ansible/vars/deploy-secrets.yml"))
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    write_terraform_vars()
    write_deploy_secrets()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
