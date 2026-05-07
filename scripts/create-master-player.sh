#!/usr/bin/env bash
# =============================================================================
# create-master-player.sh
# Creates (or re-provisions) the persistent PRO master_player account.
# Safe to run multiple times — register is idempotent (409 is handled),
# and subscription upsert always sets tier=PRO / status=ACTIVE.
#
# Usage:
#   ./scripts/create-master-player.sh
#   ./scripts/create-master-player.sh --base-url http://localhost:8080
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
RESULT_FILE="$SCRIPT_DIR/.master-player.json"
ENV_FILE="$REPO_ROOT/.env"

# ── Load credentials from root .env (never hardcoded) ─────────────────────────
if [[ -f "$ENV_FILE" ]]; then
  # shellcheck source=/dev/null
  set -o allexport && source "$ENV_FILE" && set +o allexport
else
  echo "[WARN]  $ENV_FILE not found — using defaults from .env.example"
  echo "        Copy .env.example to .env and set your values."
fi

# ── Config ────────────────────────────────────────────────────────────────────
BASE_URL="${E2E_BASE_URL:-http://localhost:8080}"
EMAIL="${MASTER_EMAIL:?'MASTER_EMAIL not set — check scripts/.env'}"
PASSWORD="${MASTER_PASSWORD:?'MASTER_PASSWORD not set — check scripts/.env'}"
DISPLAY_NAME="${MASTER_DISPLAY_NAME:-Master Player}"
STRIPE_CUSTOMER_ID="cus_master_player"
STRIPE_SUBSCRIPTION_ID="sub_master_player"

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
elif [[ "$REGISTER_HTTP" == "409" ]]; then
  warn "Account already exists — continuing."
else
  die "Registration failed (HTTP $REGISTER_HTTP): $REGISTER_BODY"
fi

# If 409, try to recover userId from previous result file
if [[ -z "$(echo "$REGISTER_BODY" | jq -r '.data.id // empty' 2>/dev/null)" && -f "$RESULT_FILE" ]]; then
  SAVED_ID=$(jq -r '.id // empty' "$RESULT_FILE" 2>/dev/null || echo "")
  [[ -n "$SAVED_ID" ]] && REGISTER_BODY="{\"data\":{\"id\":\"$SAVED_ID\"}}"
fi

# ── Step 2: Login ─────────────────────────────────────────────────────────────
info "Logging in as $EMAIL ..."
LOGIN_RESP=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")

ACCESS_TOKEN=$(echo "$LOGIN_RESP" | jq -r '.data.accessToken // empty')
[[ -n "$ACCESS_TOKEN" ]] || die "Login failed. Response: $LOGIN_RESP"
ok "Login successful. Token: ${ACCESS_TOKEN:0:30}..."

# ── Step 3: Resolve userId ────────────────────────────────────────────────────
# Prefer id from register response; fall back to /api/auth/me
USER_ID=$(echo "$REGISTER_BODY" | jq -r '.data.id // empty')

if [[ -z "$USER_ID" ]]; then
  info "Fetching userId from /api/auth/me ..."
  ME_RESP=$(curl -s "$BASE_URL/api/auth/me" \
    -H "Authorization: Bearer $ACCESS_TOKEN")
  USER_ID=$(echo "$ME_RESP" | jq -r '.data.id // empty')
  [[ -n "$USER_ID" ]] || die "Could not resolve userId. Response: $ME_RESP"
fi
ok "userId: $USER_ID"

# ── Step 4: Upsert PRO subscription ──────────────────────────────────────────
info "Granting PRO subscription ..."
SUB_RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/subscriptions" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d "{
    \"userId\":               \"$USER_ID\",
    \"tier\":                 \"PRO\",
    \"status\":               \"ACTIVE\",
    \"stripeCustomerId\":     \"$STRIPE_CUSTOMER_ID\",
    \"stripeSubscriptionId\": \"$STRIPE_SUBSCRIPTION_ID\"
  }")

SUB_BODY=$(echo "$SUB_RESP" | head -n -1)
SUB_HTTP=$(echo "$SUB_RESP" | tail -n 1)
[[ "$SUB_HTTP" == "200" || "$SUB_HTTP" == "201" ]] \
  || die "Subscription upsert failed (HTTP $SUB_HTTP): $SUB_BODY"
ok "Subscription upserted."

# ── Step 5: Verify tier ───────────────────────────────────────────────────────
info "Verifying tier ..."
TIER_RESP=$(curl -s "$BASE_URL/api/v1/subscriptions/$USER_ID/tier" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

TIER=$(echo "$TIER_RESP"  | jq -r '.data.tier   // empty')
STATUS=$(echo "$TIER_RESP" | jq -r '.data.status // empty')

[[ "$TIER" == "PRO" && "$STATUS" == "ACTIVE" ]] \
  || die "Tier verification failed. Response: $TIER_RESP"

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
echo "============================================"
echo " master_player account ready"
echo "--------------------------------------------"
echo " email    : $EMAIL"
echo " password : $PASSWORD"
echo " userId   : $USER_ID"
echo " tier     : $TIER"
echo " status   : $STATUS"
echo "============================================"

# ── Save result ───────────────────────────────────────────────────────────────
jq -n \
  --arg email "$EMAIL" \
  --arg password "$PASSWORD" \
  --arg id "$USER_ID" \
  '{"email": $email, "password": $password, "id": $id}' \
  > "$RESULT_FILE"
ok "Saved to $RESULT_FILE"
echo ""
echo "Next: run ./scripts/setup-e2e-users.sh to write .test-users.json"
