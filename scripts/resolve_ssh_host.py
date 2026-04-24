#!/usr/bin/env python3
import json
import os
import sys
from pathlib import Path


def main() -> int:
    instances_path = Path(os.environ.get("YC_INSTANCES_JSON", "/tmp/yc-instances.json"))
    vm_name = os.environ.get("VM_NAME", "")

    if not vm_name:
        print("VM_NAME is not set", file=sys.stderr)
        return 1

    with instances_path.open(encoding="utf-8") as fh:
        data = json.load(fh)

    for instance in data.get("instances", []):
        if instance.get("name") != vm_name:
            continue

        for network_interface in instance.get("networkInterfaces", []):
            primary_v4 = network_interface.get("primaryV4Address") or {}
            nat = primary_v4.get("oneToOneNat") or {}
            address = nat.get("address")
            if address:
                print(address)
                return 0

    return 1


if __name__ == "__main__":
    raise SystemExit(main())
