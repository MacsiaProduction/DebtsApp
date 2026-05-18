#!/usr/bin/env bash
# Waits for the most recent SonarCloud CE task to finish, then asserts the
# project's Quality Gate is OK. Designed to be called from CI after
# SonarSource/sonarqube-scan-action wrote .scannerwork/report-task.txt.
#
# Required env: SONAR_TOKEN
# Optional env: SONAR_HOST_URL (default https://sonarcloud.io)
#               SONAR_PROJECT_KEY (default MacsiaProduction_DebtsApp)
#               SONAR_QG_TIMEOUT_S (default 300)
set -euo pipefail

: "${SONAR_TOKEN:?SONAR_TOKEN is required}"
SONAR_HOST_URL="${SONAR_HOST_URL:-https://sonarcloud.io}"
SONAR_PROJECT_KEY="${SONAR_PROJECT_KEY:-MacsiaProduction_DebtsApp}"
SONAR_QG_TIMEOUT_S="${SONAR_QG_TIMEOUT_S:-300}"

report=".scannerwork/report-task.txt"
[ -f "$report" ] || { echo "missing $report"; exit 1; }

ce_task_url="$(grep '^ceTaskUrl=' "$report" | cut -d= -f2-)"
[ -n "$ce_task_url" ] || { echo "ceTaskUrl missing from $report"; exit 1; }

echo "Waiting for SonarCloud CE task: $ce_task_url"
deadline=$((SECONDS + SONAR_QG_TIMEOUT_S))
while :; do
  status="$(curl -sf -u "${SONAR_TOKEN}:" "$ce_task_url" \
    | python3 -c "import json,sys; print(json.load(sys.stdin)['task']['status'])")"
  echo "  CE status: $status"
  case "$status" in
    SUCCESS) break ;;
    FAILED|CANCELED) echo "CE task ended with $status"; exit 1 ;;
  esac
  [ "$SECONDS" -lt "$deadline" ] || { echo "Timeout waiting for CE task"; exit 1; }
  sleep 5
done

gate="$(curl -sf -u "${SONAR_TOKEN}:" \
  "${SONAR_HOST_URL}/api/qualitygates/project_status?projectKey=${SONAR_PROJECT_KEY}" \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['projectStatus']['status'])")"

echo "Quality Gate: $gate"
[ "$gate" = "OK" ]
