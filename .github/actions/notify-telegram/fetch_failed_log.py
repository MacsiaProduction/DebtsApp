#!/usr/bin/env python3
"""Fetch the log tail of the failing step of the current GitHub Actions job.

Reads context from environment variables (GH_REPO, GH_RUN_ID, GH_RUN_ATTEMPT,
GH_JOB, GH_SERVER_URL, GH_TOKEN) and prints up to LOG_TAIL_LINES lines from
either:
  * the section after the last "##[error]" marker in the failing step's log,
  * or the overall log tail if no such marker is present.

Designed to be best-effort: any failure prints an empty string and exits 0 so
the caller (a Telegram notification step) never fails because of a missing log.
"""
from __future__ import annotations

import json
import os
import re
import sys
import urllib.error
import urllib.request

TIMESTAMP_RE = re.compile(r"^\d{4}-\d{2}-\d{2}T[0-9:.Z+-]+ ")
ANSI_RE = re.compile(r"\x1b\[[0-9;]*[a-zA-Z]")


def http_get(url: str, token: str) -> tuple[int, bytes]:
    req = urllib.request.Request(
        url,
        headers={
            "Authorization": f"Bearer {token}",
            "Accept": "application/vnd.github+json",
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return resp.getcode(), resp.read()
    except urllib.error.HTTPError as exc:
        try:
            return exc.code, exc.read()
        except Exception:
            return exc.code, b""
    except OSError:
        return 0, b""


def find_failed_job_id(jobs_payload: bytes, job_name: str) -> int | None:
    try:
        data = json.loads(jobs_payload or b"{}")
    except ValueError:
        return None
    jobs = data.get("jobs") or []
    for j in jobs:
        if j.get("name") == job_name:
            return j.get("id")
    for j in jobs:
        if j.get("conclusion") == "failure":
            return j.get("id")
    return None


def clean(line: str) -> str:
    line = ANSI_RE.sub("", line)
    line = TIMESTAMP_RE.sub("", line)
    return line.rstrip()


def tail_for_failure(log_text: str, max_lines: int) -> str:
    lines = [clean(line) for line in log_text.splitlines()]
    last_error_idx = -1
    for i, line in enumerate(lines):
        if "##[error]" in line:
            last_error_idx = i
    if last_error_idx >= 0:
        start = max(0, last_error_idx - max_lines + 1)
        end = last_error_idx + 1
        return "\n".join(lines[start:end])
    return "\n".join(lines[-max_lines:])


def main() -> int:
    token = os.environ.get("GH_TOKEN", "")
    repo = os.environ.get("GH_REPO", "")
    run_id = os.environ.get("GH_RUN_ID", "")
    attempt = os.environ.get("GH_RUN_ATTEMPT", "1")
    job_name = os.environ.get("GH_JOB", "")
    server_url = os.environ.get("GH_SERVER_URL", "https://github.com")
    try:
        max_lines = int(os.environ.get("LOG_TAIL_LINES", "60"))
    except ValueError:
        max_lines = 60

    if not token or not repo or not run_id or not job_name:
        return 0

    api_base = server_url.replace("https://github.com", "https://api.github.com")
    jobs_url = f"{api_base}/repos/{repo}/actions/runs/{run_id}/attempts/{attempt}/jobs"
    code, payload = http_get(jobs_url, token)
    if code != 200:
        return 0

    job_id = find_failed_job_id(payload, job_name)
    if not job_id:
        return 0

    logs_url = f"{api_base}/repos/{repo}/actions/jobs/{job_id}/logs"
    code, log_bytes = http_get(logs_url, token)
    if code != 200 or not log_bytes:
        return 0

    try:
        log_text = log_bytes.decode("utf-8", errors="replace")
    except Exception:
        return 0

    sys.stdout.write(tail_for_failure(log_text, max_lines))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
