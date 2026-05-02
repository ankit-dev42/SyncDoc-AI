#!/usr/bin/env bash
# =============================================================================
# setup-e2e-users.sh
# Reads pre-created user credentials from:
#   scripts/.free-user.json       (written by create-free-user.sh)
#   scripts/.master-player.json   (written by create-master-player.sh)
#
# Logs in both accounts to obtain fresh access tokens, then writes:
#   frontend/tests/integration/fixtures/.test-users.json
#
# This file is gitignored and is the only thing the Playwright fixtures read.
# Subscription management is NOT done here — this script is purely auth.
#
# Prerequisites (run once each):
#   ./scripts/create-free-user.sh
#   ./scripts/create-master-player.sh
#
# Usage:
#   ./scripts/setup-e2e-users.sh
#   BASE_URL=http://localhost:8080 ./scripts/setup-e2e-users.sh
# =============================================================================

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
FREE_FILE="$SCRIPT_DIR/.free-user.json"
PRO_FILE="$SCRIPT_DIR/.master-player.json"
OUTPUT_FILE="$REPO_ROOT/frontend/tests/integration/fixtures/.test-users.json"

# ── Helpers ───────────────────────────────────────────────────────────────────
info()  { echo "[INFO]  $*"; }
ok()    { echo "[OK]    $*"; }
die()   { echo "[ERROR] $*" >&2; exit 1; }

require_cmd() { command -v "$1" &>/dev/null || die "'$1' is required but not installed."; }
require_cmd curl
require_cmd jq

# ── Validate prerequisite files ───────────────────────────────────────────────
[[ -f "$FREE_FILE" ]] \
  || die "Missing $FREE_FILE — run ./scripts/create-free-user.sh first."
[[ -f "$PRO_FILE" ]] \
  || die "Missing $PRO_FILE — run ./scripts/create-master-player.sh first."

FREE_EMAIL=$(jq -r '.email' "$FREE_FILE")
FREE_PASSWORD=$(jq -r '.password' "$FREE_FILE")
FREE_ID=$(jq -r '.id' "$FREE_FILE")

PRO_EMAIL=$(jq -r '.email' "$PRO_FILE")
PRO_PASSWORD=$(jq -r '.password' "$PRO_FILE")
PRO_ID=$(jq -r '.id' "$PRO_FILE")

# ── Wait for backend ──────────────────────────────────────────────────────────
info "Checking backend at $BASE_URL ..."
for i in $(seq 1 20); do
  http_code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health" 2>/dev/null || echo "000")
  if [[ "$http_code" == "200" ]]; then
    ok "Backend is UP."
    break
  fi
  if [[ $i -eq 20 ]]; then
    die "Backend not healthy after 60s."
  fi
  echo "  Waiting for backend... ($i/20)"
  sleep 3
done

# ── Login: free user ──────────────────────────────────────────────────────────
info "Logging in free user ($FREE_EMAIL) ..."
FREE_LOGIN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$FREE_EMAIL\",\"password\":\"$FREE_PASSWORD\"}")
FREE_TOKEN=$(echo "$FREE_LOGIN" | jq -r '.data.accessToken // empty')
[[ -n "$FREE_TOKEN" ]] || die "Free user login failed. Response: $FREE_LOGIN"
ok "Free user logged in."

# ── Login: pro user ───────────────────────────────────────────────────────────
info "Logging in pro user ($PRO_EMAIL) ..."
PRO_LOGIN=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$PRO_EMAIL\",\"password\":\"$PRO_PASSWORD\"}")
PRO_TOKEN=$(echo "$PRO_LOGIN" | jq -r '.data.accessToken // empty')
[[ -n "$PRO_TOKEN" ]] || die "Pro user login failed. Response: $PRO_LOGIN"
ok "Pro user logged in."

# ── Write combined .test-users.json ──────────────────────────────────────────
mkdir -p "$(dirname "$OUTPUT_FILE")"

jq -n \
  --arg free_email "$FREE_EMAIL" \
  --arg free_password "$FREE_PASSWORD" \
  --arg free_id "$FREE_ID" \
  --arg free_token "$FREE_TOKEN" \
  --arg pro_email "$PRO_EMAIL" \
  --arg pro_password "$PRO_PASSWORD" \
  --arg pro_id "$PRO_ID" \
  --arg pro_token "$PRO_TOKEN" \
  '{
    "free": {
      "email": $free_email,
      "password": $free_password,
      "id": $free_id,
      "accessToken": $free_token
    },
    "pro": {
      "email": $pro_email,
      "password": $pro_password,
      "id": $pro_id,
      "accessToken": $pro_token
    }
  }' > "$OUTPUT_FILE"

ok "Written to $OUTPUT_FILE"

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
echo "============================================"
echo " E2E test users ready"
echo "--------------------------------------------"
echo " FREE  email : $FREE_EMAIL"
echo " FREE  id    : $FREE_ID"
echo " PRO   email : $PRO_EMAIL"
echo " PRO   id    : $PRO_ID"
echo "============================================"
echo ""
echo "Run tests: cd frontend && ./node_modules/.bin/playwright test"
