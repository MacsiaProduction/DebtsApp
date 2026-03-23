#!/usr/bin/env bash
# Run Playwright UI end-to-end tests.
# Starts docker-compose (backend + DBs) and the React dev server if not already running,
# then runs the Playwright test suite in e2e/.
#
# Usage:
#   bash scripts/test-e2e-ui.sh              # headless (default)
#   bash scripts/test-e2e-ui.sh --headed     # headed browser
#   bash scripts/test-e2e-ui.sh --ui         # Playwright interactive UI mode

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND_URL="http://localhost:8080"
FRONTEND_URL="http://localhost:3000"
FRONTEND_PID=""

cleanup() {
  if [[ -n "$FRONTEND_PID" ]]; then
    echo ">>> Stopping frontend dev server (PID $FRONTEND_PID)..."
    kill "$FRONTEND_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

# ── 1. Ensure backend is running ─────────────────────────────────────────────

echo ">>> Checking backend at $BACKEND_URL ..."
if ! curl -sf "$BACKEND_URL/session" > /dev/null 2>&1; then
  echo ">>> Backend not running — starting via docker-compose ..."
  cd "$ROOT"
  docker-compose up -d --build
  echo ">>> Waiting for backend to become ready ..."
  for i in $(seq 1 40); do
    sleep 3
    if curl -sf "$BACKEND_URL/session" > /dev/null 2>&1; then
      echo ">>> Backend is ready."
      break
    fi
    echo "    still waiting... ($i/40)"
    if [[ $i -eq 40 ]]; then
      echo "ERROR: Backend did not start in time. Check docker-compose logs."
      docker-compose logs debts_bot
      exit 1
    fi
  done
else
  echo ">>> Backend already running."
fi

# ── 2. Ensure frontend dev server is running ─────────────────────────────────

echo ""
echo ">>> Checking frontend at $FRONTEND_URL ..."
if ! curl -sf "$FRONTEND_URL" > /dev/null 2>&1; then
  echo ">>> Frontend not running — starting React dev server ..."
  cd "$ROOT/frontend"

  if [[ ! -d node_modules ]]; then
    echo ">>> Installing frontend dependencies..."
    npm ci
  fi

  BROWSER=none npm start &
  FRONTEND_PID=$!

  echo ">>> Waiting for frontend to become ready ..."
  for i in $(seq 1 30); do
    sleep 2
    if curl -sf "$FRONTEND_URL" > /dev/null 2>&1; then
      echo ">>> Frontend is ready."
      break
    fi
    echo "    still waiting... ($i/30)"
    if [[ $i -eq 30 ]]; then
      echo "ERROR: Frontend did not start in time."
      exit 1
    fi
  done
else
  echo ">>> Frontend already running."
fi

# ── 3. Install Playwright deps if needed ─────────────────────────────────────

cd "$ROOT/e2e"

if [[ ! -d node_modules ]]; then
  echo ""
  echo ">>> Installing Playwright dependencies..."
  npm ci
fi

echo ">>> Ensuring Playwright browsers are installed..."
npx playwright install chromium

# ── 4. Run tests ─────────────────────────────────────────────────────────────

echo ""
echo "════════════════════════════════════════"
echo " Running Playwright E2E tests"
echo "════════════════════════════════════════"
echo ""

npx playwright test "$@"
