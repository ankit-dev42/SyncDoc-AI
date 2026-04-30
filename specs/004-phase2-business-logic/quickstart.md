# Quickstart: Phase 2 — Business Logic

**Generated**: 2026-04-20  
**Branch**: `004-phase2-business-logic`  
**Prerequisites**: Phase 1 (`003-phase1-foundation-security`) fully implemented and merged to `develop`

---

## What This Phase Delivers

After completing Phase 2:
- Stripe billing is live: checkout, portal, and webhook processing fully functional
- Subscription state is cached in Redis with 5-minute TTL and ENTERPRISE bypass
- AI documentation extraction is real (calls OpenAI, not a stub)
- Project CRUD is complete: create, list, update, and soft-delete
- Webhook deduplication prevents duplicate processing

---

## Local Development Setup

### 1. Start Infrastructure

```bash
docker-compose up -d postgres redis
```

PostgreSQL runs on `5432`; Redis runs on `6379`.

### 2. Configure Environment Variables

Copy the example env file:
```bash
cp backend/env/.env.local.properties.example backend/env/.env.local.properties
```

Add the following to your local env file:
```properties
# Stripe (use test keys from https://dashboard.stripe.com/test/apikeys)
STRIPE_API_KEY=sk_test_...
STRIPE_PUBLISHABLE_KEY=pk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...

# OpenAI (from https://platform.openai.com/api-keys)
OPENAI_API_KEY=sk-proj-...

# JWT (generate a 256-bit base64-encoded secret)
JWT_SECRET=your-base64-encoded-256-bit-secret

# Database
DB_URL=jdbc:postgresql://localhost:5432/syncdoc
DB_USERNAME=syncdoc
DB_PASSWORD=syncdoc

# GitHub Webhook
GITHUB_WEBHOOK_SECRET=your-github-webhook-secret
```

### 3. Run Database Migrations

```bash
cd backend
mvn flyway:migrate -Plocal
```

This runs V7–V11 migrations on top of the Phase 1 schema.

### 4. Start the Backend

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Backend starts on `http://localhost:8080`.

---

## Testing Stripe Webhooks Locally

Install the [Stripe CLI](https://stripe.com/docs/stripe-cli):

```bash
brew install stripe/stripe-cli/stripe
stripe login
stripe listen --forward-to localhost:8080/api/v1/billing/stripe-webhook
```

The Stripe CLI will output a webhook signing secret — use this as `STRIPE_WEBHOOK_SECRET`.

Trigger a test event:
```bash
stripe trigger checkout.session.completed
```

Expected result: `UserSubscription` created in DB with `tier=PRO`, `status=ACTIVE`.

---

## Testing AI Extraction

With the backend running:

```bash
# 1. Register a user
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"dev@example.com","password":"Password1!","displayName":"Dev"}' | jq

# 2. Login and extract JWT
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"dev@example.com","password":"Password1!"}' | jq -r '.data.accessToken')

# 3. Submit extraction
DOC_ID=$(curl -s -X POST http://localhost:8080/api/v1/ai/extract \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"sourceContent":"Added OAuth login. Removed legacy endpoint. Updated rate limiter.","sourceContentId":"commit-abc123"}' | jq -r '.data.docId')

echo "Doc ID: $DOC_ID"

# 4. Poll status (will move from PROCESSING to COMPLETED in a few seconds)
curl -s "http://localhost:8080/api/v1/ai/extract-status/$DOC_ID" \
  -H "Authorization: Bearer $TOKEN" | jq

# 5. Retrieve result
curl -s "http://localhost:8080/api/v1/ai/extract-result/$DOC_ID" \
  -H "Authorization: Bearer $TOKEN" | jq
```

---

## Testing Project CRUD

```bash
# Create a project
curl -s -X POST http://localhost:8080/api/v1/projects \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"my-repo","accessControl":"PRIVATE"}' | jq

# List projects (paginated)
curl -s "http://localhost:8080/api/v1/projects?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN" | jq

# Update a project (replace PROJECT_ID)
curl -s -X PUT http://localhost:8080/api/v1/projects/PROJECT_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"my-repo-v2","accessControl":"SHARED"}' | jq

# Delete a project (soft delete)
curl -s -X DELETE http://localhost:8080/api/v1/projects/PROJECT_ID \
  -H "Authorization: Bearer $TOKEN"
# Expected: 204 No Content
```

---

## Running Tests

```bash
# Run all backend tests
cd backend && mvn test

# Run only Phase 2 test classes
cd backend && mvn test -Dtest="StripeWebhookHandlerTest,BillingControllerIntegrationTest,\
SubscriptionServiceRedisCacheTest,AIProcessingServiceTest,\
ProjectControllerCrudIntegrationTest,WebhookIdempotencyIntegrationTest"

# Run with coverage report
cd backend && mvn verify
```

---

## Key Configuration Reference

| Property | Location | Default | Notes |
|---|---|---|---|
| `spring.ai.openai.api-key` | `application.yml` | `${OPENAI_API_KEY}` | Required for real extraction |
| `spring.ai.openai.chat.options.model` | `application.yml` | `${OPENAI_MODEL:gpt-4o-mini}` | Overridable |
| `integrations.stripe.secret-key` | `application.yml` | `${STRIPE_API_KEY:}` | Blank = billing disabled |
| `integrations.stripe.webhook-secret` | `application.yml` | `${STRIPE_WEBHOOK_SECRET:}` | Required for webhook validation |
| `spring.data.redis.host` | `application.yml` | `localhost` | Redis host |
| `spring.jpa.hibernate.ddl-auto` | `application-local.yml` | `validate` | **Must be `validate`** (fixed from `update`) |

---

## Architecture Decision Quick Reference

| Decision | Choice | Why |
|---|---|---|
| Stripe SDK init | Static `Stripe.apiKey` in `@Bean` | Idiomatic; testable via mock interface |
| Stripe webhook verify | `Webhook.constructEvent()` first | Rule 02 — verifies signature AND parses atomically |
| Redis serialisation | `Jackson2JsonRedisSerializer<Object>` | Clean JSON; no class metadata fragility |
| Soft delete | `@SQLRestriction` + `@SQLDelete` | Hibernate 6.x canonical approach; `@Where` deprecated |
| AI retry | `@Retryable(maxAttempts=3, backoff=1s×2.0)` | spring-retry; declarative, testable |
| Async error capture | try-catch in `@Async` body | Direct; `AsyncUncaughtExceptionHandler` lacks event context |
| OpenAI prompt | `.st` resource file | No magic strings in Java code |
