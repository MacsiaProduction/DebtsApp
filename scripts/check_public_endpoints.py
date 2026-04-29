#!/usr/bin/env python3
import json
import os
import socket
import sys
from pathlib import Path


def resolve_ipv4(domain: str) -> list[str]:
    try:
        records = socket.getaddrinfo(domain, None, socket.AF_INET, socket.SOCK_STREAM)
    except socket.gaierror:
        return []
    return sorted({record[4][0] for record in records})


def tcp_connect(host: str, port: int, timeout: float = 5.0) -> tuple[bool, str]:
    try:
        with socket.create_connection((host, port), timeout=timeout):
            return True, "ok"
    except OSError as exc:
        return False, str(exc)


def main() -> int:
    descriptor_path = Path(os.environ.get("HOST_DESCRIPTOR", "/tmp/host.json"))
    app_domain = os.environ["APP_DOMAIN"]
    portainer_domain = os.environ.get("PORTAINER_DOMAIN", "")

    descriptor = json.loads(descriptor_path.read_text(encoding="utf-8"))
    expected_ip = descriptor["host"]
    domains = [app_domain]
    if portainer_domain:
        domains.append(portainer_domain)

    print(f"Expected VM public IP: {expected_ip}")

    failed = False
    for domain in domains:
        addresses = resolve_ipv4(domain)
        print(f"{domain} A records: {', '.join(addresses) if addresses else '(none)'}")
        if expected_ip not in addresses:
            print(
                f"ERROR: {domain} does not point to deployed VM {expected_ip}. "
                f"Update the DNS A record.",
                file=sys.stderr,
            )
            failed = True

    for port in (80, 443):
        ok, reason = tcp_connect(expected_ip, port)
        print(f"{expected_ip}:{port} public TCP check: {reason}")
        if not ok:
            print(
                f"ERROR: deployed VM {expected_ip} does not accept public TCP/{port}. "
                "Check Caddy, Docker port publishing, VM firewall, and cloud network rules.",
                file=sys.stderr,
            )
            failed = True

    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
