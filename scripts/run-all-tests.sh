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
(
  cd "$FRONTEND_DIR"
  npm run test:e2e
)

log_step "All backend and frontend tests passed"
