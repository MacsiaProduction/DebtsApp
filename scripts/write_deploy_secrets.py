#!/usr/bin/env python3
# Формирует YAML-файл с секретами для Ansible из переменных окружения.
import os
from pathlib import Path


def yaml_quote(value: str) -> str:
    value = value or ""
    return '"' + value.replace("\\", "\\\\").replace('"', '\\"') + '"'


def main() -> int:
    lines = [
        f"app_domain: {yaml_quote(os.environ.get('APP_DOMAIN', 'debtsapp2.macsia.fun'))}",
        f"acme_email: {yaml_quote(os.environ.get('ACME_EMAIL', 'macsia.production@gmail.com'))}",
        f"postgres_password: {yaml_quote(os.environ['POSTGRES_PASSWORD'])}",
        f"neo4j_password: {yaml_quote(os.environ['NEO4J_PASSWORD'])}",
        f"jwt_secret: {yaml_quote(os.environ['JWT_SECRET'])}",
        f"bot_token: {yaml_quote(os.environ.get('BOT_TOKEN', ''))}",
    ]
    output_path = Path(
        os.environ.get("DEPLOY_SECRETS_OUTPUT", "infra/ansible/vars/deploy-secrets.yml")
    )
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
