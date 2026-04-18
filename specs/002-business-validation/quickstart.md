# Quickstart Guide: SyncDoc AI Business Validation & Deployment Readiness

**Phase**: Phase 1 — Local development setup and manual testing  
**Duration**: ~15 minutes to full working environment  
**Version**: 1.0  
**Date**: 2026-04-14  

---

## Prerequisites

- Java 17 JDK installed (`java -version` returns 17.x)
- Maven 3.8+ (`mvn -version`)
- Docker + Docker Compose (`docker --version` and `docker-compose --version`)
- Node.js 18+ + npm (`node -v` and `npm -v`)
- Git (`git --version`)
- A Stripe test-mode account (for payment E2E test)
- PostgreSQL 16 client tools (optional, for direct schema inspection)

---

## 1. Local Environment Setup (5 minutes)

### Start Backend Infrastructure

```bash
cd /path/to/syncdoc-ai
docker-compose up -d postgres redis
```

**Verification**:
```bash
# Postgres should be reachable on localhost:5432
psql -h localhost -U postgres -d collaboration -c "SELECT version();"

# Redis should be reachable on localhost:6379
redis-cli -p 6379 ping  # Response: PONG
```

### Run Database Migrations

```bash
cd backend
mvn flyway:info  # List current schema version
mvn flyway:migrate  # Apply pending migrations

# Verify tables created
mvn flyway:info | grep "| "
```

### Backend: Start Spring Boot Server

```bash
cd backend
mvn clean spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"

# Server starts on http://localhost:8080
# Check console output for: "Started Application in ... seconds"
```

### Frontend: Start Vite Dev Server

```bash
cd ../frontend
npm install  # First time only
npm run dev

# Dev server on http://localhost:5173
# Console output: "VITE vX.X.X  ready in XXX ms"
```

---

## 2. Integration Setup (5 minutes)

### Configure Stripe Test Mode (for payment E2E test)

```bash
# Create .env in frontend/ with test credentials
cat > frontend/.env.local << 'EOF'
VITE_STRIPE_PUBLISHABLE_KEY=pk_test_xxxxxxxxxxxxxxxxxxxx
VITE_STRIPE_SECRET_KEY=sk_test_xxxxxxxxxxxxxxxxxxxx  # Not exposed to frontend; backend only
EOF

# Retrieve from Stripe Dashboard → Developers → API Keys
```

### Configure OpenAI Mock (for AI extraction tests)

```bash
# Backend will use mocked OpenAI for non-E2E tests
# For manual testing, use a real OpenAI test account:

cat > backend/src/test/resources/mock-openai-responses.json << 'EOF'
[
  {
    "prompt": "Extract key changes from: ...",
    "response": "## Key Changes\n- Feature A\n## Action Items\n- TODO: Document B"
  }
]
EOF
```

---

## 3. Manual Integration Testing (5 minutes)

### Test 1: Subscription Entitlement Check

```bash
# Create a test user subscription
curl -X POST http://localhost:8080/api/v1/subscriptions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -d '{
    "userId": "test-user-123",
    "tier": "PRO",
    "stripeCustomerId": "cus_test123",
    "status": "ACTIVE"
  }'

# Check tier
curl http://localhost:8080/api/v1/subscriptions/test-user-123/tier \
  -H "Authorization: Bearer <JWT_TOKEN>"

# Response: { "userId": "test-user-123", "tier": "PRO", "status": "ACTIVE" }

# Test sync authorization
curl -X POST http://localhost:8080/api/v1/subscriptions/test-user-123/can-sync \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -d '{
    "userId": "test-user-123",
    "repositoryUrl": "https://github.com/test/repo",
    "currentSyncCount": 0
  }'

# Response: { "authorized": true, "reason": "Active subscription allows sync" }
```

### Test 2: Project Access Control

```bash
# Create a project (as owner)
curl -X POST http://localhost:8080/api/v1/projects \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT_USER_1>" \
  -d '{
    "ownerId": "user-1",
    "name": "My Documentation",
    "accessControl": "PRIVATE"
  }'

# Response includes: { "id": "proj-abc123", "ownerId": "user-1", ... }

# Access as owner (should succeed)
curl http://localhost:8080/api/v1/projects/proj-abc123 \
  -H "Authorization: Bearer <JWT_USER_1>"
# Response: 200 OK + project data

# Access as another user (should fail with 403)
curl http://localhost:8080/api/v1/projects/proj-abc123 \
  -H "Authorization: Bearer <JWT_USER_2>"
# Response: 403 Forbidden
```

### Test 3: GitHub Webhook Verification

```bash
# Generate valid HMAC-SHA256 signature
PAYLOAD='{"action":"opened","pull_request":{"id":123}}'
SECRET="your-github-webhook-secret"
SIGNATURE=$(echo -n "$PAYLOAD" | openssl dgst -sha256 -hmac "$SECRET" -hex | cut -d' ' -f2)

# Send valid webhook
curl -X POST http://localhost:8080/api/v1/webhooks/github \
  -H "Content-Type: application/json" \
  -H "X-Hub-Signature-256: sha256=$SIGNATURE" \
  -d "$PAYLOAD"

# Response: 202 Accepted

# Send invalid webhook (wrong signature)
curl -X POST http://localhost:8080/api/v1/webhooks/github \
  -H "Content-Type: application/json" \
  -H "X-Hub-Signature-256: sha256=invalidsignaturehere" \
  -d "$PAYLOAD"

# Response: 403 Forbidden (signature verification failed)
```

### Test 4: AI Documentation Extraction

```bash
# Submit content for extraction
curl -X POST http://localhost:8080/api/v1/ai/extract \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -d '{
    "userId": "test-user-123",
    "sourceContent": "## Overview\nWe changed the authentication system. Users can now login with OAuth2. Action item: update docs."
  }'

# Response: { "docId": "doc-xyz789", "status": "PENDING" }

# Poll extraction status
curl http://localhost:8080/api/v1/ai/extract-status/doc-xyz789 \
  -H "Authorization: Bearer <JWT_TOKEN>"

# Response: { "docId": "doc-xyz789", "status": "COMPLETED" } (after ~3s)

# Get extraction result
curl http://localhost:8080/api/v1/ai/extract-result/doc-xyz789 \
  -H "Authorization: Bearer <JWT_TOKEN>"

# Response: { "keyChanges": "Users can now login with OAuth2", "actionItems": "Update docs" }
```

---

## 4. Automated Test Execution

### Backend: Unit Tests

```bash
cd backend

# Run subscription tests
mvn -Dtest=SubscriptionServiceTest test

# Run project access tests
mvn -Dtest=ProjectAccessServiceSecurityTest test

# Run webhook signature verification tests
mvn -Dtest=WebhookSignatureVerificationTest test

# Run all business validation tests
mvn -Dtest=*SubscriptionTest,*ProjectTest,*WebhookTest,*AIProcessingTest test
```

### Backend: Integration Tests

```bash
# Requires PostgreSQL + Redis running
mvn verify -Dgroups="integration"
```

### Frontend: Unit Tests

```bash
cd ../frontend

# Run component and hook tests
npm run test -- src/features/billing src/features/project src/features/webhook src/features/ai

# Run with coverage
npm run test -- --coverage
```

### Frontend: E2E Tests (Payment Success Path)

```bash
# Requires Stripe test-mode credentials in .env.local
npm run test:e2e -- payment-success.spec.ts

# Running single test:
# → Opens browser
# → Logs in as test user
# → Clicks "Upgrade to Pro"
# → Completes test-card payment (4242 4242 4242 4242)
# → Verifies redirect to success page
# → Takes screenshots on failure
```

---

## 5. Deployment-Readiness Checks

### Java 17 Compliance Verification

```bash
cd backend
mvn test -Dtest=Java17CompatibilityTest

# Output: "✅ All bytecode is Java 17 compatible (version 61)"
```

### Secret Hygiene Scan

```bash
cd backend
mvn test -Dtest=SecretExposureAuditTest

# Output: "✅ No hardcoded secrets detected"
# Or: "❌ Hardcoded secrets found at: [list of files and patterns]"
```

### Database Migration Integrity

```bash
cd backend
mvn test -Dtest=UserSubscriptionsMigrationIntegrationTest

# Verifies user_subscriptions schema is present and valid
# Output: "✅ user_subscriptions table migrated successfully"
```

### Webhook Dispatch Latency

```bash
cd backend
mvn test -Dtest=WebhookDispatchPerformanceTest

# Verifies p95 receipt-to-publish latency stays <=100ms
```

### Multi-Tenant Leak Audit

```bash
cd backend
mvn test -Dtest=MultiTenantSecurityAuditTest

# Executes 1000 unauthorized cross-project requests
# Output: all responses must be 403 Forbidden
```

### AI Extraction Accuracy Gate

```bash
cd backend
mvn test -Dtest=AIExtractionAccuracyGateTest

# Validates 20 fixture pairs and average similarity >= 0.95
```

### Full Phase 8 Gate Run

```bash
cd backend
mvn -Dtest=Java17CompatibilityTest,SecretExposureAuditTest,UserSubscriptionsMigrationIntegrationTest,WebhookDispatchPerformanceTest,MultiTenantSecurityAuditTest,AIExtractionAccuracyGateTest test
```

**Validation Evidence**:
- Java compatibility gate inspects compiled bytecode in `target/classes` and asserts major version 61.
- Secret hygiene gate scans backend/frontend source trees for quoted secret literals instead of environment-driven configuration.
- Migration integrity gate asserts the V3 migration contains the expected `user_subscriptions` schema and indexes.
- Webhook latency gate measures controller receipt through event publication over 100 samples.
- Multi-tenant audit executes 1000 unauthorized project requests and requires 1000 `403` responses.
- AI accuracy gate validates the 20-case fixture corpus and enforces average similarity >= 0.95.

---

## 6. Troubleshooting

| Issue | Solution |
|---|---|
| **"Connection refused" to PostgreSQL** | Ensure `docker-compose up -d postgres` is running; check `docker ps` |
| **Maven compilation error with Java 21 features** | Verify `java -version` returns 17.x; check pom.xml `<java.version>17</java.version>` |
| **Frontend WebSocket connection fails** | Backend must be running on port 8080; firewall may block if not localhost |
| **Stripe test card rejected** | Use only Stripe test-mode credentials; live keys will fail with "endpoint not found" |
| **Webhook signature mismatch** | Ensure secret matches GitHub webhook configuration; HMAC is case-sensitive |
| **AI extraction timeout (OpenAI test)** | Mock fallback is used; real OpenAI calls require valid API key in `.env.production` |

---

## 7. Next Steps

→ Proceed to `/speckit.implement` to execute task-based implementation
→ Monitor health checks: `curl http://localhost:8080/health/java17` and `curl http://localhost:8080/health/secrets`
