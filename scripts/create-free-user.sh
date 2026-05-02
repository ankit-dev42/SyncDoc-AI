#!/usr/bin/env bash
# =============================================================================
# create-free-user.sh
# Creates (or re-provisions) the persistent FREE test user.
# No subscription is granted — the account remains on the free tier.
#
# Run this ONCE before running E2E tests for the first time, or after a DB wipe.
# Safe to re-run: 409 on register is handled gracefully.
#
# Saves credentials + userId to scripts/.free-user.json (gitignored)
# so that setup-e2e-users.sh can read them later.
#
# Usage:
#   ./scripts/create-free-user.sh
#   BASE_URL=http://localhost:8080 ./scripts/create-free-user.sh
# =============================================================================

set -euo pipefail

# ── Config ────────────────────────────────────────────────────────────────────
BASE_URL="${BASE_URL:-http://localhost:8080}"
EMAIL="free_tester@syncdoc.dev"
PASSWORD="FreeTester!2026"
DISPLAY_NAME="Free Tester"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESULT_FILE="$SCRIPT_DIR/.free-user.json"

# ── Helpers ───────────────────────────────────────────────────────────────────
info()  { echo "[INFO]  $*"; }
ok()    { echo "[OK]    $*"; }
warn()  { echo "[WARN]  $*"; }
die()   { echo "[ERROR] $*" >&2; exit 1; }

require_cmd() { command -v "$1" &>/dev/null || die "'$1' is required but not installed."; }
require_cmd curl
require_cmd jq

# ── Wait for backend ──────────────────────────────────────────────────────────
info "Checking backend at $BASE_URL ..."
for i in $(seq 1 20); do
  http_code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health" 2>/dev/null || echo "000")
  if [[ "$http_code" == "200" ]]; then
    ok "Backend is UP."
    break
  fi
  if [[ $i -eq 20 ]]; then
    die "Backend did not become healthy after 60s. Start it first."
  fi
  echo "  Waiting for backend... ($i/20)"
  sleep 3
done

# ── Step 1: Register ──────────────────────────────────────────────────────────
info "Registering $EMAIL ..."
REGISTER_RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\",\"displayName\":\"$DISPLAY_NAME\"}")

REGISTER_BODY=$(echo "$REGISTER_RESP" | head -n -1)
REGISTER_HTTP=$(echo "$REGISTER_RESP" | tail -n 1)

if [[ "$REGISTER_HTTP" == "200" || "$REGISTER_HTTP" == "201" ]]; then
  ok "Registered successfully."
  USER_ID=$(echo "$REGISTER_BODY" | jq -r '.data.id // empty')
elif [[ "$REGISTER_HTTP" == "409" ]]; then
  warn "Account already exists — continuing."
  # userId will be read from the existing result file below
  USER_ID=""
else
  die "Registration failed (HTTP $REGISTER_HTTP): $REGISTER_BODY"
fi

# ── Step 2: Resolve userId from existing result file if not captured ──────────
if [[ -z "$USER_ID" && -f "$RESULT_FILE" ]]; then
  USER_ID=$(jq -r '.id // empty' "$RESULT_FILE" 2>/dev/null || echo "")
  [[ -n "$USER_ID" ]] && info "Using stored userId: $USER_ID"
fi

[[ -n "$USER_ID" ]] || die "Could not resolve userId. Try deleting $RESULT_FILE and re-running."

# ── Step 3: Verify login works ────────────────────────────────────────────────
info "Verifying login ..."
LOGIN_RESP=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")

ACCESS_TOKEN=$(echo "$LOGIN_RESP" | jq -r '.data.accessToken // empty')
[[ -n "$ACCESS_TOKEN" ]] || die "Login failed. Response: $LOGIN_RESP"
ok "Login verified."

# ── Step 4: Save result ───────────────────────────────────────────────────────
jq -n \
  --arg email "$EMAIL" \
  --arg password "$PASSWORD" \
  --arg id "$USER_ID" \
  '{"email": $email, "password": $password, "id": $id}' \
  > "$RESULT_FILE"

ok "Saved to $RESULT_FILE"

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
echo "============================================"
echo " free_tester account ready"
echo "--------------------------------------------"
echo " email    : $EMAIL"
echo " password : $PASSWORD"
echo " userId   : $USER_ID"
echo " tier     : FREE (no subscription granted)"
echo "============================================"
echo ""
echo "Next: run ./scripts/setup-e2e-users.sh to write .test-users.json"
