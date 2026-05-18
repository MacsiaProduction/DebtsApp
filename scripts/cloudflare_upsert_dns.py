#!/usr/bin/env python3
"""Upsert Cloudflare A-records pointing the project subdomains to the VM IP.

Touches only records explicitly listed in CF_RECORDS to avoid affecting unrelated
records hosted in the same Cloudflare zone.

Environment:
    CLOUDFLARE_API_TOKEN  Cloudflare API token with Zone:Read + DNS:Edit on the zone.
    CF_ZONE_NAME          Base zone (default: macsia.fun).
    CF_RECORDS            Comma-separated FQDNs to upsert (e.g. "a.app.macsia.fun,b.app.macsia.fun").
    CF_TARGET_IP          IPv4 address every record should point to.
    CF_PROXIED            "true"/"false" — whether to proxy through Cloudflare (default: false).
    CF_TTL                TTL in seconds (default: 60). Ignored when proxied=true.
"""
from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.request

CF_API = "https://api.cloudflare.com/client/v4"


def cf_request(method: str, path: str, token: str, payload: dict | None = None) -> dict:
    body = json.dumps(payload).encode("utf-8") if payload is not None else None
    req = urllib.request.Request(
        f"{CF_API}{path}",
        method=method,
        data=body,
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            data = json.load(resp)
    except urllib.error.HTTPError as exc:
        detail = exc.read().decode("utf-8", "replace")
        raise SystemExit(f"Cloudflare API {method} {path} failed: {exc.code} {detail}") from exc
    if not data.get("success", False):
        raise SystemExit(f"Cloudflare API {method} {path} not successful: {data}")
    return data


def find_zone_id(token: str, zone_name: str) -> str:
    data = cf_request("GET", f"/zones?name={zone_name}", token)
    result = data.get("result") or []
    if not result:
        raise SystemExit(f"Cloudflare zone {zone_name!r} not found for this token")
    return result[0]["id"]


def find_record(token: str, zone_id: str, fqdn: str) -> dict | None:
    data = cf_request("GET", f"/zones/{zone_id}/dns_records?type=A&name={fqdn}", token)
    result = data.get("result") or []
    return result[0] if result else None


def upsert_record(token: str, zone_id: str, fqdn: str, ip: str, *, proxied: bool, ttl: int) -> None:
    payload = {
        "type": "A",
        "name": fqdn,
        "content": ip,
        "ttl": 1 if proxied else ttl,
        "proxied": proxied,
        "comment": "managed by DebtsApp CI",
    }
    existing = find_record(token, zone_id, fqdn)
    if existing is None:
        cf_request("POST", f"/zones/{zone_id}/dns_records", token, payload)
        print(f"created {fqdn} -> {ip} (proxied={proxied})")
        return
    if existing.get("content") == ip and existing.get("proxied") == proxied:
        print(f"unchanged {fqdn} (already -> {ip}, proxied={proxied})")
        return
    cf_request("PUT", f"/zones/{zone_id}/dns_records/{existing['id']}", token, payload)
    print(f"updated {fqdn} -> {ip} (was {existing.get('content')}, proxied={proxied})")


def verify_token(token: str) -> None:
    data = cf_request("GET", "/user/tokens/verify", token)
    status = (data.get("result") or {}).get("status", "")
    print(f"Cloudflare token verified, status={status}")


def main() -> int:
    token = os.environ.get("CLOUDFLARE_API_TOKEN") or os.environ.get("CLOUDFLARE_TOKEN", "")
    if not token:
        print("CLOUDFLARE_API_TOKEN is not set", file=sys.stderr)
        return 1

    zone_name = os.environ.get("CF_ZONE_NAME", "macsia.fun")
    records_raw = os.environ.get("CF_RECORDS", "")
    target_ip = os.environ.get("CF_TARGET_IP", "")
    proxied = os.environ.get("CF_PROXIED", "false").lower() in {"1", "true", "yes"}
    ttl = int(os.environ.get("CF_TTL", "60"))

    if not target_ip:
        print("CF_TARGET_IP is not set", file=sys.stderr)
        return 1

    fqdns = [r.strip() for r in records_raw.split(",") if r.strip()]
    if not fqdns:
        print("CF_RECORDS is empty", file=sys.stderr)
        return 1

    # Safety: refuse to touch anything that isn't a subdomain of the configured zone.
    for fqdn in fqdns:
        if not (fqdn == zone_name or fqdn.endswith("." + zone_name)):
            print(f"refusing to touch {fqdn}: outside zone {zone_name}", file=sys.stderr)
            return 1

    verify_token(token)
    zone_id = find_zone_id(token, zone_name)
    print(f"Resolved zone {zone_name} -> {zone_id}")
    for fqdn in fqdns:
        try:
            upsert_record(token, zone_id, fqdn, target_ip, proxied=proxied, ttl=ttl)
        except SystemExit as exc:
            msg = str(exc)
            if "10000" in msg or "Authentication error" in msg:
                print(
                    "\nHINT: the Cloudflare API token can list zones but cannot read/edit DNS records.\n"
                    "Recreate the token at https://dash.cloudflare.com/profile/api-tokens with:\n"
                    "  • Permissions: Zone -> DNS -> Edit\n"
                    "  • Zone Resources: Include -> Specific zone -> macsia.fun\n"
                    "Then update the CLOUDFLARE_TOKEN GitHub secret.",
                    file=sys.stderr,
                )
            raise
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
