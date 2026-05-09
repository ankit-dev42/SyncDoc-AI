#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_DIR="$ROOT_DIR/frontend"
PLAYWRIGHT_CACHE_DIR="$HOME/Library/Caches/ms-playwright"

log_step() {
  printf '\n==> %s\n' "$1"
}

ensure_playwright_chromium() {
  if find "$PLAYWRIGHT_CACHE_DIR" -maxdepth 1 -name 'chromium-*' -print -quit 2>/dev/null | grep -q .; then
    return 0
  fi

  log_step "Installing Playwright Chromium"
  (
    cd "$FRONTEND_DIR"
    npx playwright install chromium
  )
}

log_step "Running backend test suite"
(
  cd "$BACKEND_DIR"
  mvn test
)

log_step "Running frontend unit/component test suite"
(
  cd "$FRONTEND_DIR"
  npm test -- --run
)

ensure_playwright_chromium

log_step "Running frontend Playwright E2E suite"
if curl -sf --max-time 3 http://localhost:8080/actuator/health > /dev/null 2>&1; then
  (
    cd "$FRONTEND_DIR"
    npm run test:e2e
  )
else
  printf '\n[WARN] Backend not reachable at localhost:8080 — skipping E2E integration tests.\n'
  printf '       Start the backend (and its dependencies) first, then re-run:\n'
  printf '         cd frontend && npm run test:e2e\n'
fi

log_step "All backend and frontend tests passed"
