# SyncDoc AI

> Enterprise-grade real-time collaboration platform with AI-powered documentation synchronisation

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.2-blue.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.2-blue.svg)](https://www.typescriptlang.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-red.svg)](https://redis.io/)
[![Elasticsearch](https://img.shields.io/badge/Elasticsearch-8.11-yellow.svg)](https://www.elastic.co/)

SyncDoc AI combines instant messaging, presence tracking, threaded conversations, full-text search, GitHub webhook ingestion, OpenAI-powered documentation extraction, and Stripe subscription billing — all in a single production-hardened Java + React application.

---

## Table of Contents

1. [Features](#features)
2. [Tech Stack](#tech-stack)
3. [Architecture Overview](#architecture-overview)
4. [System Flow Diagrams](#system-flow-diagrams)
5. [Project Structure](#project-structure)
6. [Prerequisites](#prerequisites)
7. [Quick Start](#quick-start)
8. [Environment Configuration](#environment-configuration)
9. [API Reference](#api-reference)
   - [Authentication](#authentication-api)
   - [Workspaces](#workspaces-api)
   - [Messaging](#messaging-api)
   - [Presence](#presence-api)
   - [Search](#search-api)
   - [Projects](#projects-api)
   - [Subscriptions](#subscriptions-api)
   - [Billing](#billing-api)
   - [AI Extraction](#ai-extraction-api)
   - [Webhooks](#webhooks-api)
10. [WebSocket Reference](#websocket-reference)
11. [Database Schema](#database-schema)
12. [Security Model](#security-model)
13. [Testing](#testing)
14. [CI/CD Pipeline](#cicd-pipeline)
15. [Development Roadmap](#development-roadmap)

---

## Features

| Feature | Description |
|---------|-------------|
| **JWT Auth + Token Rotation** | Stateless access tokens (15 min) with HttpOnly-cookie refresh tokens (7 days), rotated on every use |
| **Real-Time Messaging** | STOMP over WebSocket — sub-50 ms delivery; Redis pub/sub for horizontal scale |
| **Sequence-Ordered Messages** | Atomic Redis counter per channel guarantees ordering and gap-free replay |
| **Idempotent Sends** | Per-channel idempotency keys prevent duplicate messages on mobile retries |
| **Threaded Conversations** | Replies attached to a parent message with thread-level aggregation |
| **Presence Tracking** | Per-workspace ONLINE / AWAY / OFFLINE with heartbeat API and timezone-aware last-seen |
| **Full-Text Search** | Elasticsearch-backed search with sender, time-range filters, and snippet highlighting |
| **GitHub Webhook Ingestion** | HMAC-SHA-256 signature verification, SHA-256 payload deduplication, async dispatch |
| **AI Documentation Generation** | OpenAI gpt-4o-mini extracts key changes, action items, and quality score from code diffs |
| **Stripe Billing** | Checkout sessions, customer portal, webhook processing with idempotent event deduplication |
| **Multi-Tenancy** | Workspace-scoped isolation enforced by `WorkspaceMembershipFilter` on every protected route |
| **Rate Limiting** | `RateLimitFilter` applied before auth — stops brute-force before any DB hit |

---

## Tech Stack

### Backend

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 17 LTS | Core language |
| Spring Boot | 3.2.0 | Framework + auto-configuration |
| Spring Security | 6.x | JWT filter chain, method-level `@PreAuthorize` |
| Spring Data JPA / Hibernate | 6.x | ORM; Flyway-managed schema (validate mode) |
| Spring Data Redis | 3.x | Caching, pub/sub, sequence counters |
| Spring Data Elasticsearch | 5.x | Full-text search indexing |
| Spring AI | 1.0.0-M6 | OpenAI (gpt-4o-mini) integration |
| Spring WebSocket (STOMP) | 6.x | Real-time messaging at `/ws` |
| PostgreSQL | 16 | Primary relational store |
| Redis | 7 (Alpine) | Cache and WebSocket broadcast scaling |
| Elasticsearch | 8.11.0 | Message full-text search |
| Flyway | Latest | 12 versioned SQL migrations (V1–V12) |
| JJWT | 0.12.6 | JWT generation and validation |
| Stripe Java SDK | 23.3.0 | Checkout, portal, webhook validation |
| JaCoCo | Latest | Code coverage — 45% minimum enforced in CI |

### Frontend

| Technology | Version | Purpose |
|------------|---------|---------|
| React | 18.2 | UI library |
| TypeScript | 5.2 | Type safety |
| Vite | 5.0 | Build tool, sub-second HMR |
| Tailwind CSS | 3.4 | Utility-first styling |
| Zustand | 4.4 | Global state (auth, workspace) with persistence middleware |
| Axios | 1.6 | HTTP client with JWT-attach and 401-refresh interceptors |
| TanStack React Query | 5.x | Server-state caching, background refetch |
| React Router DOM | 6.30 | Client-side routing with protected route guard |
| React Hook Form | 7.74 | Form state management |
| Vitest | 1.0 | Unit + component tests (jsdom) — 60% coverage minimum |
| Playwright | 1.40 | E2E tests (main + develop branches only) |

---

## Architecture Overview

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                               CLIENT LAYER                                        │
│                                                                                    │
│   Browser / React SPA  (Vite 5 · TypeScript 5 · Tailwind CSS)                    │
│   ├── authStore (Zustand, sessionStorage) — accessToken + user                   │
│   ├── workspaceStore (Zustand, localStorage) — activeWorkspaceId, members        │
│   ├── Axios client — Bearer attach · 401 refresh-lock · workspace headers        │
│   ├── TanStack React Query — presence · threads · search                         │
│   └── STOMP WebSocket client — /ws (SockJS fallback)                             │
└──────────────────────────┬───────────────────────────────────────────────────────┘
                           │  HTTP/REST  +  WebSocket (STOMP/SockJS)
                           ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│                          SECURITY FILTER CHAIN                                    │
│                                                                                    │
│   ① RateLimitFilter           — IP-rate-limit; blocks brute-force before auth    │
│   ② JwtAuthenticationFilter   — validates Bearer token; populates SecurityContext│
│   ③ WorkspaceMembershipFilter — workspace isolation on protected routes           │
│                                                                                    │
│   CORS: locked to FRONTEND_ORIGIN env var (no wildcard)                          │
│   Session: STATELESS — no HTTP session created                                   │
└──────────────────────────┬───────────────────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│                       APPLICATION LAYER  (Spring Boot 3.2)                        │
│                                                                                    │
│  /api/auth          /api/v1/workspaces   /api/v1/workspaces/{id}/channels/{id}/  │
│  Register           List members         messages — CRUD, threads, search, count  │
│  Login              Add/remove member                                             │
│  Refresh                                 /ws  (STOMP)                             │
│  Logout             /api/v1/projects     send · edit · delete · presence          │
│                     CRUD (owner-scoped)                                           │
│  /api/v1/workspaces/{id}/presence        /api/v1/billing                         │
│  Status · Heartbeat · Unread count       Checkout · Portal · Stripe webhook      │
│                                                                                    │
│  /api/v1/workspaces/{id}/search          /api/v1/subscriptions                   │
│  Elasticsearch full-text + filters       Tier · Can-sync · Upsert                │
│                                                                                    │
│  /api/v1/ai                              /api/v1/webhooks/github                  │
│  Submit · Status · Result                HMAC verify → dedup → async dispatch    │
└────────────┬──────────────────────────────────────────────────────────────────────┘
             │
    ┌────────┼──────────────┐
    ▼        ▼              ▼
┌────────┐ ┌──────────┐ ┌──────────────────┐
│Postgres│ │ Redis 7  │ │Elasticsearch 8.11│
│   16   │ │          │ │                  │
│users   │ │sequences │ │messages index    │
│messages│ │pub/sub   │ │(full-text +      │
│presence│ │rate-limit│ │ snippet)         │
│subscr. │ │counters  │ │                  │
│projects│ │          │ │                  │
│refresh │ │          │ │                  │
│tokens  │ │          │ │                  │
│webhook │ │          │ │                  │
│events  │ │          │ │                  │
│gen_doc │ │          │ │                  │
└────────┘ └──────────┘ └──────────────────┘
```

---

## System Flow Diagrams

### Authentication Flow

```
Client                    AuthController          AuthService               DB
  │                            │                       │                     │
  ├─POST /api/auth/register────►│                       │                     │
  │  {email, password,          │──register(req)────────►│                     │
  │   displayName}              │                       │──bcrypt hash──────►│
  │                             │                       │◄─user saved──────────│
  │◄─201 {id, email}────────────│◄──UserResponse─────────│                     │
  │                             │                       │                     │
  ├─POST /api/auth/login────────►│                       │                     │
  │  {email, password}          │──login(req)───────────►│──verify pw────────►│
  │                             │                       │──gen accessToken    │
  │                             │                       │──gen refreshToken   │
  │                             │                       │──save refreshToken►│
  │◄─200 {accessToken}──────────│◄──TokenResponse────────│                     │
  │  Set-Cookie: refresh_token  │  +Set-Cookie HttpOnly  │                     │
  │  (HttpOnly, Strict, 7 days) │                       │                     │
  │                             │                       │                     │
  │  [15 min later — 401]       │                       │                     │
  ├─POST /api/auth/refresh──────►│                       │                     │
  │  Cookie: refresh_token      │──refresh(token)───────►│──validate──────────►│
  │                             │                       │──rotate: delete old│
  │                             │                       │──issue new pair───►│
  │◄─200 {accessToken}──────────│◄──TokenResponse────────│                     │
  │  Set-Cookie: new refresh    │  +Set-Cookie HttpOnly  │                     │
  │                             │                       │                     │
  ├─POST /api/auth/logout───────►│                       │                     │
  │  Cookie: refresh_token      │──logout(token)────────►│──delete token─────►│
  │◄─200 Logged out─────────────│◄──ok───────────────────│                     │
  │  Set-Cookie: (cleared)      │                       │                     │
```

### Real-Time Messaging Flow

```
Client A (Sender)         Backend                   Redis              Client B
     │                       │                         │                    │
     ├─STOMP CONNECT──────────►│                         │                    │
     │  /ws (SockJS)          │                         │                    │
     ├─SUBSCRIBE──────────────►│                         │                    │
     │  /topic/workspace/     │                         │                    │
     │  W1/channel/C1/msgs    │                         │                    │
     │                        │                    ─────────────────────────►│
     │                        │              (Client B subscribes same topic)│
     ├─SEND───────────────────►│                         │                    │
     │  /app/workspace/W1/    │──INCR seq counter───────►│                    │
     │  channel/C1/send       │◄─sequenceNumber──────────│                    │
     │  {content,             │──save Message(PG)        │                    │
     │   idempotencyKey}      │──index to ES             │                    │
     │                        │──PUBLISH────────────────►│                    │
     │                        │◄─broadcast───────────────│                    │
     │◄─/topic/.../messages───│                          │──deliver──────────►│
     │  {id, content, seq,    │                          │  {id, content,     │
     │   senderId, ...}       │                          │   seq, senderId}   │
     │                        │                          │                    │
     │     [REST for history sync]                       │                    │
     ├─GET .../messages/after/42──────────────────────────────────────────────│
     │◄─[{seq:43,...},{seq:44,...}]────────────────────────────────────────────│
```

### GitHub Webhook → AI Documentation Flow

```
GitHub                WebhookController       WebhookEventDispatcher   AIProcessingService
  │                         │                         │                        │
  ├─POST /api/v1/webhooks/  │                         │                        │
  │  github                 │                         │                        │
  │  X-Hub-Signature-256:.. │                         │                        │
  │  X-GitHub-Event: push   │                         │                        │
  │                         │──HMAC-SHA-256 verify     │                        │
  │                         │  FAIL ──────────────────────► 400 + audit log    │
  │                         │                         │                        │
  │                         │──SHA-256(payload)        │                        │
  │                         │──existsByPayloadHash?    │                        │
  │                         │  YES ───────────────────────► 202 (duplicate)    │
  │                         │                         │                        │
  │                         │──webhookAuditService     │                        │
  │                         │  .recordAccepted()       │                        │
  │◄─202 Accepted───────────│                         │                        │
  │                         │──dispatch(async)─────────►│                        │
  │                         │                         │──route by eventType    │
  │                         │                         │──validate schema       │
  │                         │                         │──processExtraction────►│
  │                         │                         │                        │──OpenAI API
  │                         │                         │                        │  gpt-4o-mini
  │                         │                         │                        │──parse result
  │                         │                         │                        │──save GeneratedDoc
  │                         │                         │                        │  {keyChanges,
  │                         │                         │                        │   actionItems,
  │                         │                         │                        │   qualityScore}
```

### Stripe Billing Flow

```
Client              BillingController        StripeClient            Stripe API
  │                       │                       │                       │
  ├─POST /api/v1/billing/ │                       │                       │
  │  checkout             │──createCheckout────────►│──create session──────►│
  │  {priceId}            │                       │◄─{url, sessionId}─────│
  │  Bearer: <token>      │                       │                       │
  │◄─200 {checkoutUrl,────│◄──CheckoutResponse─────│                       │
  │       sessionId}      │                       │                       │
  │                       │                       │                       │
  ├─redirect → checkoutUrl─────────────────────────────────────────────────►│
  │                       │                       │            [user pays] │
  │                       │                       │                       │
  │         Stripe sends POST /api/v1/billing/stripe-webhook               │
  │         Stripe-Signature: t=...,v1=...                                 │
  │                       │──verify signature      │                       │
  │                       │──handle(async)         │                       │
  │                       │  SubscriptionService   │                       │
  │                       │  .upsert()             │                       │
  │                       │◄─200 OK────────────────│                       │
  │                       │                        │                       │
  ├─GET /api/v1/billing/  │                        │                       │
  │  portal               │──getPortal(customerId)─►│──create portal session►│
  │◄─200 {portalUrl}──────│◄──PortalResponse────────│◄─{url}────────────────│
```

---

## Project Structure

```
syncdoc-ai/
├── backend/
│   ├── pom.xml                                  # Spring Boot 3.2.0, Java 17
│   └── src/main/java/com/syncdoc/collaboration/
│       ├── CollaborationApplication.java
│       ├── config/              # WebSecurityConfig, WebSocketConfig, RedisConfig, AsyncConfig
│       ├── security/            # JwtAuthenticationFilter, RateLimitFilter
│       ├── tenancy/             # WorkspaceMembershipFilter, TenantScopedRepository
│       ├── auth/                # Register · Login · Refresh · Logout
│       ├── messaging/           # Messages, Threads, WebSocket STOMP handler
│       ├── presence/            # Status · Heartbeat · Unread count
│       ├── billing/             # Checkout · Portal · Stripe webhook handler
│       ├── subscription/        # Tier · Can-sync · Upsert; Stripe client stub
│       ├── ai/                  # Submit · Status · Result; OpenAI client
│       ├── webhook/             # GitHub ingest · HMAC verify · async dispatch
│       ├── project/             # Project CRUD (owner-scoped)
│       ├── search/              # Elasticsearch full-text search
│       ├── workspace/           # Workspace + member management
│       ├── sync/                # Sync log model
│       ├── observability/       # Metrics, AuditLogger, HealthIndicator
│       ├── common/              # BaseEntity, ApiResponse, PagedResponse
│       └── exception/           # GlobalExceptionHandler, domain exceptions
│   └── src/main/resources/
│       ├── application.yml      # Base config
│       ├── application-local.yml
│       ├── application-test.yml
│       └── db/migration/        # Flyway V1–V12
├── frontend/
│   ├── vite.config.ts           # Vitest (jsdom), 60% coverage thresholds
│   ├── tsconfig.json            # Strict, ESNext, isolatedModules
│   └── src/
│       ├── api/
│       │   ├── client.ts        # Axios: JWT inject, 401 refresh-lock
│       │   ├── contextHeaders.ts
│       │   └── messageApi.ts
│       ├── components/
│       │   ├── ProtectedRoute.tsx
│       │   ├── NotFoundPage.tsx
│       │   └── messaging/       # MessageInput, MessageItem, MessageList
│       └── features/
│           ├── auth/            # LoginPage, RegisterPage, authStore (sessionStorage)
│           ├── workspace/       # WorkspaceList, workspaceStore (localStorage)
│           ├── presence/        # PresenceBadge, usePresence hook
│           ├── messaging/       # ThreadList, Thread, useThreads hook
│           ├── search/          # SearchBox, SearchResults, useSearch hook
│           └── billing/         # PricingTable, BillingPage, ManageSubscription
├── .github/workflows/
│   ├── ci.yml                   # Tests + build on push/PR
│   └── pr-checks.yml            # Checkstyle + ESLint on PR
├── docker-compose.yml           # postgres:16, redis:7-alpine, elasticsearch:8.11.0
├── .env.example
├── docs/
├── specs/
└── tests/                       # E2E (Playwright)
```

---

## Prerequisites

| Tool | Minimum Version |
|------|----------------|
| Java (Eclipse Temurin) | 17 |
| Maven | 3.8 |
| Node.js | 18 |
| npm | 9 |
| Docker + Docker Compose v2 | 24 |

---

## Quick Start

### 1. Clone and configure

```bash
git clone https://github.com/ankit-dev42/SyncDoc-AI.git
cd syncdoc-ai
cp .env.example .env
# Fill in JWT_SECRET (≥32 chars), Stripe keys, OpenAI key, GitHub secrets
```

### 2. Start infrastructure

```bash
docker compose up -d
# postgres  → localhost:5433
# redis     → localhost:6379
# elastic   → localhost:9200
```

### 3. Start the backend

```bash
cd backend
mvn spring-boot:run
# http://localhost:8080
```

### 4. Start the frontend

```bash
cd frontend
npm install
npm run dev
# http://localhost:5173
```

### 5. Smoke test

```bash
# Health (no auth needed)
curl -s http://localhost:8080/actuator/health | jq .

# Register
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"secret123","displayName":"Alice"}' | jq .

# Login — saves the refresh-token cookie and captures the access token
TOKEN=$(curl -sc /tmp/cookies.txt -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"secret123"}' \
  | jq -r '.data.accessToken')

# Verify token works
curl -s http://localhost:8080/api/v1/workspaces \
  -H "Authorization: Bearer $TOKEN" | jq .
```

---

## Environment Configuration

### Backend

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `DB_URL` | Yes | — | `jdbc:postgresql://localhost:5433/collaboration` |
| `DB_USERNAME` | Yes | — | PostgreSQL username |
| `DB_PASSWORD` | Yes | — | PostgreSQL password |
| `JWT_SECRET` | Yes | — | Random string ≥ 32 characters |
| `FRONTEND_ORIGIN` | No | `http://localhost:5173` | CORS allowed origin |
| `STRIPE_API_KEY` | Billing | — | `sk_test_...` / `sk_live_...` |
| `STRIPE_PUBLISHABLE_KEY` | Billing | — | `pk_test_...` / `pk_live_...` |
| `STRIPE_WEBHOOK_SECRET` | Billing | — | `whsec_...` from Stripe dashboard |
| `GITHUB_CLIENT_ID` | GitHub OAuth | — | OAuth App client ID |
| `GITHUB_CLIENT_SECRET` | GitHub OAuth | — | OAuth App client secret |
| `GITHUB_WEBHOOK_SECRET` | Webhooks | — | HMAC secret set on GitHub repo |
| `OPENAI_API_KEY` | AI features | — | OpenAI API key |

**Token lifetimes** (configurable in `application.yml`):

```yaml
jwt:
  access-token-expiry-minutes: 15   # Bearer token
  refresh-token-expiry-days: 7      # HttpOnly cookie, rotated on each use
```

### Frontend (`frontend/.env`)

| Variable | Default | Description |
|----------|---------|-------------|
| `VITE_API_BASE_URL` | `http://localhost:8080/api` | Backend base URL |

---

## API Reference

All protected endpoints require `Authorization: Bearer <accessToken>`.

**Standard response envelope:**

```json
{
  "success": true,
  "message": "Human-readable message",
  "data": { ... }
}
```

**Paginated response:**

```json
{
  "content": [...],
  "page": 0,
  "size": 50,
  "totalElements": 137,
  "totalPages": 3,
  "last": false
}
```

---

### Authentication API

Base: `/api/auth` — no JWT required on any route.

#### Register

```
POST /api/auth/register
Content-Type: application/json
```

| Field | Type | Rules |
|-------|------|-------|
| `email` | string | valid email, required |
| `password` | string | 8–128 chars, required |
| `displayName` | string | max 100 chars, optional |

```bash
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "password": "secret123",
    "displayName": "Alice"
  }' | jq .
```

**201 Created:**

```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "alice@example.com",
    "displayName": "Alice"
  }
}
```

---

#### Login

```
POST /api/auth/login
Content-Type: application/json
```

| Field | Type | Rules |
|-------|------|-------|
| `email` | string | valid email, required |
| `password` | string | required |

```bash
curl -sc /tmp/cookies.txt -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"secret123"}' | jq .
```

**200 OK:**

```json
{
  "success": true,
  "message": "Login successful",
  "data": { "accessToken": "eyJhbGciOiJIUzI1NiJ9..." }
}
```

> `refresh_token` is set as an `HttpOnly; SameSite=Strict` cookie (7-day TTL).
> `-sc /tmp/cookies.txt` saves it for subsequent calls.

---

#### Refresh access token

```
POST /api/auth/refresh
Cookie: refresh_token=<value>
```

```bash
curl -sb /tmp/cookies.txt -sc /tmp/cookies.txt \
  -X POST http://localhost:8080/api/auth/refresh | jq .
```

**200 OK:** same shape as login. Old refresh token is **immediately revoked** and a new one is set.

---

#### Logout

```
POST /api/auth/logout
Cookie: refresh_token=<value>
```

```bash
curl -sb /tmp/cookies.txt -X POST http://localhost:8080/api/auth/logout | jq .
```

**200 OK:** `{"success":true,"message":"Logged out successfully","data":null}`

---

### Workspaces API

Base: `/api/v1/workspaces` — JWT required.

#### List my workspaces

```
GET /api/v1/workspaces
Authorization: Bearer <token>
```

```bash
curl -s http://localhost:8080/api/v1/workspaces \
  -H "Authorization: Bearer $TOKEN" | jq .
```

**200 OK:**

```json
{
  "success": true,
  "message": "Workspace list",
  "data": [
    { "id": "ws-001", "name": "Workspace ws-001", "memberCount": 3 }
  ]
}
```

---

#### Add a member

```
POST /api/v1/workspaces/{workspaceId}/members/{userId}
Authorization: Bearer <token>
```

```bash
curl -s -X POST \
  "http://localhost:8080/api/v1/workspaces/ws-001/members/user-bob" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

---

#### Remove a member

```
DELETE /api/v1/workspaces/{workspaceId}/members/{userId}
Authorization: Bearer <token>
```

```bash
curl -s -X DELETE \
  "http://localhost:8080/api/v1/workspaces/ws-001/members/user-bob" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

---

### Messaging API

Base: `/api/v1/workspaces/{workspaceId}/channels/{channelId}/messages` — JWT required.

#### Send a message

```
POST /api/v1/workspaces/{workspaceId}/channels/{channelId}/messages
Authorization: Bearer <token>
Content-Type: application/json
```

| Field | Type | Rules |
|-------|------|-------|
| `senderId` | string | max 50, required |
| `content` | string | max 10,000 chars, required |
| `idempotencyKey` | string | max 100, optional — prevents duplicate sends |
| `parentMessageId` | string | max 50, optional — creates a thread reply |

```bash
curl -s -X POST \
  "http://localhost:8080/api/v1/workspaces/ws-001/channels/general/messages" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "senderId": "user-alice",
    "content": "Hello, team!",
    "idempotencyKey": "msg-2024-001"
  }' | jq .
```

**200 OK:**

```json
{
  "success": true,
  "message": "Message sent successfully",
  "data": {
    "id": "msg-uuid-123",
    "workspaceId": "ws-001",
    "channelId": "general",
    "senderId": "user-alice",
    "content": "Hello, team!",
    "sequenceNumber": 42,
    "messageType": "USER",
    "parentMessageId": null,
    "editedAt": null,
    "createdAt": "2024-01-15T10:30:00Z"
  }
}
```

---

#### List messages (paginated, newest last)

```
GET .../messages?page=0&size=50
Authorization: Bearer <token>
```

```bash
curl -s "http://localhost:8080/api/v1/workspaces/ws-001/channels/general/messages?page=0&size=50" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

---

#### Get messages after a sequence (incremental sync)

```
GET .../messages/after/{sequenceNumber}
Authorization: Bearer <token>
```

```bash
curl -s \
  "http://localhost:8080/api/v1/workspaces/ws-001/channels/general/messages/after/42" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

**200 OK:** `{ "data": [{ "sequenceNumber": 43, ... }, { "sequenceNumber": 44, ... }] }`

---

#### Get messages before a sequence (history load)

```
GET .../messages/before/{sequenceNumber}?limit=50
Authorization: Bearer <token>
```

```bash
curl -s \
  "http://localhost:8080/api/v1/workspaces/ws-001/channels/general/messages/before/100?limit=50" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

---

#### Edit a message

```
PUT .../messages/{messageId}
Authorization: Bearer <token>
Content-Type: application/json
```

| Field | Type | Rules |
|-------|------|-------|
| `content` | string | max 10,000, required |
| `editorId` | string | max 50, required |

```bash
curl -s -X PUT \
  "http://localhost:8080/api/v1/workspaces/ws-001/channels/general/messages/msg-uuid-123" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"content":"Hello, team! (edited)","editorId":"user-alice"}' | jq .
```

---

#### Delete a message (soft delete)

```
DELETE .../messages/{messageId}?deleterId={userId}
Authorization: Bearer <token>
```

```bash
curl -s -X DELETE \
  "http://localhost:8080/api/v1/workspaces/ws-001/channels/general/messages/msg-uuid-123?deleterId=user-alice" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

---

#### In-channel full-text search

```
GET .../messages/search?query=<term>&page=0&size=20
Authorization: Bearer <token>
```

```bash
curl -s \
  "http://localhost:8080/api/v1/workspaces/ws-001/channels/general/messages/search?query=deployment&page=0&size=20" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

---

#### Get thread replies

```
GET .../messages/thread/{parentMessageId}
Authorization: Bearer <token>
```

```bash
curl -s \
  "http://localhost:8080/api/v1/workspaces/ws-001/channels/general/messages/thread/msg-uuid-123" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

---

#### List all threads in a channel

```
GET .../messages/threads
Authorization: Bearer <token>
```

```bash
curl -s \
  "http://localhost:8080/api/v1/workspaces/ws-001/channels/general/messages/threads" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

---

#### Get a thread by root message ID

```
GET .../messages/threads/{rootMessageId}
Authorization: Bearer <token>
```

```bash
curl -s \
  "http://localhost:8080/api/v1/workspaces/ws-001/channels/general/messages/threads/msg-uuid-123" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

---

#### Get message count

```
GET .../messages/count
Authorization: Bearer <token>
```

```bash
curl -s \
  "http://localhost:8080/api/v1/workspaces/ws-001/channels/general/messages/count" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

**200 OK:** `{ "data": 137 }`

---

### Presence API

Base: `/api/v1/workspaces/{workspaceId}/presence` — JWT required.

#### Set presence status

```
POST /api/v1/workspaces/{workspaceId}/presence/status
Authorization: Bearer <token>
Content-Type: application/json
```

| Field | Type | Values |
|-------|------|--------|
| `userId` | string | required |
| `status` | enum | `ONLINE` \| `AWAY` \| `OFFLINE` |

```bash
curl -s -X POST \
  "http://localhost:8080/api/v1/workspaces/ws-001/presence/status" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"userId":"user-alice","status":"ONLINE"}' | jq .
```

**200 OK:**

```json
{
  "success": true,
  "message": "Presence updated",
  "data": {
    "workspaceId": "ws-001",
    "userId": "user-alice",
    "status": "ONLINE",
    "lastSeenAt": "2024-01-15T10:30:00Z",
    "timezoneId": "UTC"
  }
}
```

---

#### Heartbeat (keep-alive)

```
POST /api/v1/workspaces/{workspaceId}/presence/heartbeat?userId={userId}
Authorization: Bearer <token>
```

```bash
curl -s -X POST \
  "http://localhost:8080/api/v1/workspaces/ws-001/presence/heartbeat?userId=user-alice" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

---

#### List all workspace presence

```
GET /api/v1/workspaces/{workspaceId}/presence
Authorization: Bearer <token>
```

```bash
curl -s "http://localhost:8080/api/v1/workspaces/ws-001/presence" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

---

#### Unread message count

```
GET /api/v1/workspaces/{workspaceId}/presence/unread-count
  ?channelId={id}&lastReadSequence={seq}
Authorization: Bearer <token>
```

```bash
curl -s \
  "http://localhost:8080/api/v1/workspaces/ws-001/presence/unread-count?channelId=general&lastReadSequence=40" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

**200 OK:** `{ "data": 4 }`

---

### Search API

Base: `/api/v1/workspaces/{workspaceId}/search` — JWT required.  
Backed by Elasticsearch 8.11 with snippet highlighting.

#### Full-text search

```
GET /api/v1/workspaces/{workspaceId}/search
  ?query=<term>
  &channelId=<id>
  [&from=<senderId>]
  [&before=<ISO-8601>]
  [&after=<ISO-8601>]
  [&page=0]
  [&size=20]
Authorization: Bearer <token>
```

```bash
# Basic search
curl -s \
  "http://localhost:8080/api/v1/workspaces/ws-001/search?query=kubernetes&channelId=devops" \
  -H "Authorization: Bearer $TOKEN" | jq .

# Time-range filter
curl -s \
  "http://localhost:8080/api/v1/workspaces/ws-001/search?query=deploy&channelId=devops&after=2024-01-01T00:00:00Z&before=2024-01-31T23:59:59Z" \
  -H "Authorization: Bearer $TOKEN" | jq .

# Sender filter
curl -s \
  "http://localhost:8080/api/v1/workspaces/ws-001/search?query=fix&channelId=general&from=user-bob" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

**200 OK:**

```json
{
  "success": true,
  "message": "Search results",
  "data": [
    {
      "messageId": "msg-uuid-99",
      "workspaceId": "ws-001",
      "channelId": "devops",
      "senderId": "user-bob",
      "snippet": "...rolling out to <em>kubernetes</em> cluster now...",
      "createdAt": "2024-01-15T09:15:00Z",
      "sequenceNumber": 88
    }
  ]
}
```

---

### Projects API

Base: `/api/v1/projects` — JWT required. Projects are owner-scoped (owner UUID comes from the JWT).

#### Create a project

```
POST /api/v1/projects
Authorization: Bearer <token>
Content-Type: application/json
```

| Field | Type | Rules |
|-------|------|-------|
| `name` | string | required |
| `accessControl` | string | e.g. `PRIVATE`, `PUBLIC` |

```bash
curl -s -X POST http://localhost:8080/api/v1/projects \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Alpha Service","accessControl":"PRIVATE"}' | jq .
```

**201 Created:**

```json
{
  "success": true,
  "message": "Project created",
  "data": {
    "id": "proj-uuid-001",
    "ownerId": "550e8400-...",
    "name": "Alpha Service",
    "accessControl": "PRIVATE",
    "createdAt": "2024-01-15T10:00:00Z"
  }
}
```

---

#### List my projects

```
GET /api/v1/projects?page=0&size=20
Authorization: Bearer <token>
```

```bash
curl -s "http://localhost:8080/api/v1/projects" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

---

#### Get a project

```
GET /api/v1/projects/{projectId}
Authorization: Bearer <token>
```

```bash
curl -s "http://localhost:8080/api/v1/projects/proj-uuid-001" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

---

#### Update a project

```
PUT /api/v1/projects/{projectId}
Authorization: Bearer <token>
Content-Type: application/json
```

```bash
curl -s -X PUT "http://localhost:8080/api/v1/projects/proj-uuid-001" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Alpha Service v2","accessControl":"PRIVATE"}' | jq .
```

---

#### Delete a project

```
DELETE /api/v1/projects/{projectId}
Authorization: Bearer <token>
```

```bash
curl -s -X DELETE "http://localhost:8080/api/v1/projects/proj-uuid-001" \
  -H "Authorization: Bearer $TOKEN" -w "\nHTTP %{http_code}\n"
# 204 No Content
```

---

### Subscriptions API

Base: `/api/v1/subscriptions` — JWT required.

#### Get subscription tier

```
GET /api/v1/subscriptions/{userId}/tier
Authorization: Bearer <token>
```

```bash
curl -s "http://localhost:8080/api/v1/subscriptions/user-alice-uuid/tier" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

**200 OK:**

```json
{
  "success": true,
  "message": "Subscription tier retrieved",
  "data": {
    "tier": "PRO",
    "status": "ACTIVE",
    "expiresAt": "2025-01-15T00:00:00Z"
  }
}
```

**Tier values:** `FREE` | `PRO` | `ENTERPRISE`  
**Status values:** `ACTIVE` | `CANCELED` | `EXPIRED` | `PAST_DUE`

---

#### Check sync authorisation

```
POST /api/v1/subscriptions/{userId}/can-sync
Authorization: Bearer <token>
Content-Type: application/json
```

| Field | Type | Rules |
|-------|------|-------|
| `currentSyncCount` | int | ≥ 0 |

```bash
curl -s -X POST \
  "http://localhost:8080/api/v1/subscriptions/user-alice-uuid/can-sync" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"currentSyncCount":5}' | jq .
```

**200 OK:**

```json
{
  "success": true,
  "message": "Sync authorization evaluated",
  "data": {
    "authorized": true,
    "reason": "Within PRO tier limit",
    "recommendedAction": null
  }
}
```

---

#### Upsert subscription (internal / webhook-driven)

```
POST /api/v1/subscriptions
Authorization: Bearer <token>
Content-Type: application/json
```

| Field | Type | Rules |
|-------|------|-------|
| `userId` | string | required |
| `tier` | enum | `FREE` \| `PRO` \| `ENTERPRISE` |
| `status` | enum | `ACTIVE` \| `CANCELED` \| `EXPIRED` \| `PAST_DUE` |
| `stripeCustomerId` | string | required |
| `stripeSubscriptionId` | string | optional |

```bash
curl -s -X POST http://localhost:8080/api/v1/subscriptions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-alice-uuid",
    "tier": "PRO",
    "status": "ACTIVE",
    "stripeCustomerId": "cus_xxx",
    "stripeSubscriptionId": "sub_xxx"
  }' | jq .
```

---

### Billing API

Base: `/api/v1/billing` — `/checkout` and `/portal` require JWT; `/stripe-webhook` is public (Stripe-signature verified).

#### Create Stripe checkout session

```
POST /api/v1/billing/checkout
Authorization: Bearer <token>
Content-Type: application/json
```

| Field | Type | Description |
|-------|------|-------------|
| `priceId` | string | Stripe price ID |

```bash
curl -s -X POST http://localhost:8080/api/v1/billing/checkout \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"priceId":"price_1NxHCGLkdIwHu7ixPro"}' | jq .
```

**200 OK:**

```json
{
  "checkoutUrl": "https://checkout.stripe.com/pay/cs_test_...",
  "sessionId": "cs_test_..."
}
```

> Redirect the browser to `checkoutUrl` to complete payment.

---

#### Get customer portal URL

```
GET /api/v1/billing/portal
Authorization: Bearer <token>
```

```bash
curl -s http://localhost:8080/api/v1/billing/portal \
  -H "Authorization: Bearer $TOKEN" | jq .
```

**200 OK:** `{ "portalUrl": "https://billing.stripe.com/session/..." }`

---

#### Stripe webhook receiver (public)

```
POST /api/v1/billing/stripe-webhook
Stripe-Signature: t=...,v1=...
Content-Type: application/json
```

```bash
# Test locally with the Stripe CLI
stripe listen --forward-to http://localhost:8080/api/v1/billing/stripe-webhook
```

**200 OK** on valid signature. **400 Bad Request** on invalid signature.

---

### AI Extraction API

Base: `/api/v1/ai` — JWT required.  
Extraction is **asynchronous**: Submit → poll status → fetch result.

#### Submit content for AI extraction

```
POST /api/v1/ai/extract
Authorization: Bearer <token>
Content-Type: application/json
```

| Field | Type | Rules |
|-------|------|-------|
| `sourceContentId` | string | max 50 chars, required |
| `sourceContent` | string | max 100,000 chars, required |

```bash
curl -s -X POST http://localhost:8080/api/v1/ai/extract \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "sourceContentId": "commit-abc123",
    "sourceContent": "diff --git a/src/Auth.java b/src/Auth.java\n+++ b/src/Auth.java\n@@ -10,6 +10,8 @@\n+ public void newMethod() { return; }"
  }' | jq .
```

**202 Accepted:**

```json
{
  "success": true,
  "message": "Extraction submitted",
  "data": { "docId": "doc-uuid-001", "status": "PROCESSING" }
}
```

> If the internal queue is full: **503 Service Unavailable** with `Retry-After: 30` header.

---

#### Poll extraction status

```
GET /api/v1/ai/extract-status/{docId}
Authorization: Bearer <token>
```

```bash
curl -s "http://localhost:8080/api/v1/ai/extract-status/doc-uuid-001" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

**200 OK:**

```json
{
  "success": true,
  "message": "Extraction status retrieved",
  "data": { "docId": "doc-uuid-001", "status": "PROCESSING" }
}
```

**Status values:** `PROCESSING` | `COMPLETED` | `FAILED`

---

#### Fetch extraction result

```
GET /api/v1/ai/extract-result/{docId}
Authorization: Bearer <token>
```

```bash
curl -s "http://localhost:8080/api/v1/ai/extract-result/doc-uuid-001" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

**200 OK (COMPLETED):**

```json
{
  "success": true,
  "message": "Extraction result retrieved",
  "data": {
    "docId": "doc-uuid-001",
    "status": "COMPLETED",
    "keyChanges": "Added newMethod() to handle edge case in authentication flow.",
    "actionItems": "1. Update unit tests\n2. Update API documentation",
    "qualityScore": 0.87
  }
}
```

---

### Webhooks API

Base: `/api/v1/webhooks` — public, authenticated via HMAC-SHA-256 signature.

#### Receive a GitHub webhook event

```
POST /api/v1/webhooks/github
X-Hub-Signature-256: sha256=<hmac>
X-GitHub-Event: <event-type>
X-GitHub-Delivery: <uuid>
Content-Type: application/json
```

> Configure this URL in your GitHub repo → Settings → Webhooks.  
> Set the same secret as `GITHUB_WEBHOOK_SECRET`.

```bash
# Simulate locally
SECRET="your-webhook-secret"
PAYLOAD='{"ref":"refs/heads/main","commits":[{"id":"abc","message":"fix: auth bug"}]}'
SIG="sha256=$(echo -n "$PAYLOAD" | openssl dgst -sha256 -hmac "$SECRET" | awk '{print $2}')"

curl -s -X POST http://localhost:8080/api/v1/webhooks/github \
  -H "Content-Type: application/json" \
  -H "X-Hub-Signature-256: $SIG" \
  -H "X-GitHub-Event: push" \
  -H "X-GitHub-Delivery: test-delivery-001" \
  -d "$PAYLOAD" | jq .
```

**202 Accepted** (new event): `{ "message": "Webhook accepted", "data": null }`  
**202 Accepted** (duplicate): `{ "message": "Webhook already processed", "data": null }`  
**400 Bad Request** (invalid signature): `{ "code": "INVALID_WEBHOOK_SIGNATURE" }`

---

## WebSocket Reference

Connect to `ws://localhost:8080/ws` via STOMP over SockJS.

### Connect

```javascript
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const client = new Client({
  webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
  connectHeaders: { Authorization: `Bearer ${accessToken}` },
  onConnect: () => console.log('Connected'),
});
client.activate();
```

### Subscribe to channel messages

```javascript
client.subscribe(
  '/topic/workspace/ws-001/channel/general/messages',
  (frame) => {
    const msg = JSON.parse(frame.body);
    // { id, content, sequenceNumber, senderId, workspaceId, channelId, createdAt }
  }
);
```

### Send a message

```javascript
// STOMP destinations are prefixed /app — maps to @MessageMapping
client.publish({
  destination: '/app/workspace/ws-001/channel/general/send',
  body: JSON.stringify({
    content: 'Hello via WebSocket!',
    idempotencyKey: `msg-${Date.now()}`,
  }),
});
```

### Edit a message

```javascript
client.publish({
  destination: '/app/workspace/ws-001/channel/general/edit',
  body: JSON.stringify({ messageId: 'msg-uuid-123', content: 'Edited content' }),
});
```

### Delete a message

```javascript
client.publish({
  destination: '/app/workspace/ws-001/channel/general/delete',
  body: JSON.stringify({ messageId: 'msg-uuid-123' }),
});
```

### Update presence

```javascript
client.publish({
  destination: '/app/workspace/ws-001/presence',
  body: JSON.stringify({ status: 'AWAY' }),
  // ONLINE | AWAY | OFFLINE
});
```

---

## Database Schema

Schema is **validated** on startup (`ddl-auto: validate`). All changes go through Flyway (V1–V12).

```
users
  id              UUID          PK
  email           VARCHAR(255)  UNIQUE NOT NULL
  password_hash   VARCHAR(255)  NOT NULL
  display_name    VARCHAR(100)
  email_verified  BOOLEAN       DEFAULT false
  created_at      TIMESTAMPTZ   NOT NULL
  updated_at      TIMESTAMPTZ   NOT NULL

user_subscriptions
  id                  UUID         PK
  user_id             UUID         UNIQUE FK→users.id
  subscription_tier   VARCHAR(20)  NOT NULL  [FREE|PRO|ENTERPRISE]
  stripe_customer_id  VARCHAR(100) NOT NULL
  status              VARCHAR(20)  NOT NULL  [ACTIVE|CANCELED|EXPIRED|PAST_DUE]
  created_at          TIMESTAMPTZ  NOT NULL
  renewed_at          TIMESTAMPTZ
  expires_at          TIMESTAMPTZ

refresh_tokens
  id          UUID         PK
  user_id     UUID         FK→users.id
  token_hash  VARCHAR(255) UNIQUE NOT NULL
  expires_at  TIMESTAMPTZ  NOT NULL
  revoked     BOOLEAN      DEFAULT false
  created_at  TIMESTAMPTZ  NOT NULL

messages
  id                VARCHAR(50)   PK
  workspace_id      VARCHAR(50)   NOT NULL  IDX(workspace_id, channel_id)
  channel_id        VARCHAR(50)   NOT NULL
  sender_id         VARCHAR(50)   NOT NULL
  content           TEXT          NOT NULL  (max 10 KB)
  sequence_number   BIGINT        NOT NULL
  idempotency_key   VARCHAR(100)  UNIQUE per (workspace, channel, key)
  message_type      VARCHAR(20)   [USER|SYSTEM|AI_GENERATED]
  parent_message_id VARCHAR(50)   NULL (thread reply)
  edited_at         TIMESTAMPTZ
  deleted_at        TIMESTAMPTZ   NULL (soft delete)
  created_at        TIMESTAMPTZ   NOT NULL
  updated_at        TIMESTAMPTZ   NOT NULL

message_threads
  id               UUID        PK
  workspace_id     VARCHAR(50) NOT NULL
  channel_id       VARCHAR(50) NOT NULL
  root_message_id  VARCHAR(50) UNIQUE NOT NULL
  reply_count      INT         DEFAULT 0
  last_reply_at    TIMESTAMPTZ
  created_at       TIMESTAMPTZ NOT NULL
  updated_at       TIMESTAMPTZ NOT NULL

presence
  id            UUID        PK
  workspace_id  VARCHAR(50) NOT NULL
  user_id       VARCHAR(50) NOT NULL
  UNIQUE (workspace_id, user_id)
  status        VARCHAR(20) [ONLINE|AWAY|OFFLINE]
  manual_status VARCHAR(20)
  last_seen_at  TIMESTAMPTZ
  timezone_id   VARCHAR(50) DEFAULT 'UTC'
  created_at    TIMESTAMPTZ NOT NULL
  updated_at    TIMESTAMPTZ NOT NULL

projects
  id             UUID        PK
  owner_id       UUID        FK→users.id
  name           VARCHAR(255) NOT NULL
  access_control VARCHAR(50)
  deleted_at     TIMESTAMPTZ  NULL (soft delete)
  created_at     TIMESTAMPTZ  NOT NULL
  updated_at     TIMESTAMPTZ  NOT NULL

webhook_events
  id            UUID        PK
  delivery_id   VARCHAR(255) NOT NULL
  event_type    VARCHAR(100) NOT NULL
  payload_hash  VARCHAR(64)  UNIQUE NOT NULL  (SHA-256 dedup key)
  payload       TEXT         NOT NULL (JSONB snapshot)
  status        VARCHAR(20)
  created_at    TIMESTAMPTZ  NOT NULL

processed_stripe_events
  id               UUID        PK
  stripe_event_id  VARCHAR(255) UNIQUE NOT NULL  (idempotency)
  processed_at     TIMESTAMPTZ NOT NULL

generated_documentation
  id                UUID        PK
  user_id           UUID        FK→users.id
  source_content_id VARCHAR(50)
  status            VARCHAR(20) [PROCESSING|COMPLETED|FAILED]
  key_changes       TEXT
  action_items      TEXT
  quality_score     DOUBLE
  created_at        TIMESTAMPTZ NOT NULL
  updated_at        TIMESTAMPTZ NOT NULL

sync_logs
  id           UUID        PK
  project_id   UUID        FK→projects.id
  status       VARCHAR(20)
  triggered_at TIMESTAMPTZ NOT NULL
  completed_at TIMESTAMPTZ
```

---

## Security Model

### Request Filter Order

```
Incoming request
       │
       ▼
  ① RateLimitFilter
       IP-based limit; rejects before any auth logic touches the DB
       │
       ▼
  ② JwtAuthenticationFilter
       Validates Authorization: Bearer token
       Populates SecurityContext
       Returns 401 if token missing/expired/invalid on protected routes
       │
       ▼
  ③ WorkspaceMembershipFilter
       Verifies caller is a member of the workspaceId in the URL path
       Returns 403 if not a member
       │
       ▼
  Controller / Service
```

### Public Routes (no JWT)

| Pattern | Auth mechanism |
|---------|---------------|
| `POST /api/auth/**` | None (open) |
| `POST /api/v1/webhooks/**` | HMAC-SHA-256 (GitHub secret) |
| `POST /api/v1/billing/stripe-webhook` | Stripe-Signature header |
| `GET /actuator/health` | None (open) |
| `GET /actuator/info` | None (open) |
| `WS /ws/**` | None for handshake; STOMP header auth |

### CORS

Locked to `FRONTEND_ORIGIN` env var. Wildcard `*` is never used. Credentials allowed (required for the HttpOnly cookie).

### Token Details

| Token | Delivery | TTL | Rotation |
|-------|----------|-----|----------|
| Access token | JSON response body | 15 minutes | — |
| Refresh token | `HttpOnly; SameSite=Strict` cookie | 7 days | Rotated on every use; old token revoked immediately |

Passwords: BCrypt (strength 10).

---

## Testing

### Backend

```bash
cd backend

# All tests
mvn test

# With JaCoCo coverage report (target/site/jacoco/index.html)
mvn verify

# CI profile — enforces 45% minimum coverage
mvn verify -Pci

# Code style check (Checkstyle)
mvn checkstyle:check
```

**Coverage exclusions** (per `pom.xml`): `**/config/**`, `**/model/**`, `**/dto/**`, `**/exception/**`, `**/CollaborationApplication.java`

### Frontend

```bash
cd frontend

# Unit + component tests (Vitest / jsdom)
npm test

# With coverage report (coverage/index.html)
npm run test:coverage

# Lint (ESLint + TypeScript)
npm run lint

# Production build
npm run build
```

**Coverage thresholds** (per `vite.config.ts`): 60% statements, branches, functions, and lines.  
Coverage applies to Phase 3 code. Legacy Phase 1–2 components are excluded.

### E2E (Playwright)

```bash
# Start full stack
docker compose up -d
cd backend && mvn spring-boot:run &
cd frontend && npm run dev &

# Run E2E
cd frontend && npm run test:e2e
```

E2E tests run in CI only on `main` and `develop` branches.

---

## CI/CD Pipeline

### ci.yml — triggered on every push and PR

```
push / pull_request
         │
    ┌────┴──────────────────────┐
    ▼                           ▼
backend-test               frontend-test
────────────               ─────────────
Java 17 (Temurin)          Node 20
PostgreSQL 16 (service)    npm ci
Redis 7 (service)          npm run lint
mvn verify -Pci            npm run test:coverage
JaCoCo report (artifact)   npm run build
         │                       │
         └─────────┬─────────────┘
                   ▼
              e2e  (main + develop only)
              ──────────────────────────
              Node 20
              Install Playwright browsers
              docker compose up -d
              sleep 15s
              npm run test:e2e
              docker compose down
```

### pr-checks.yml — triggered on every PR

```
pull_request
      │
 ┌────┴────────────────┐
 ▼                     ▼
backend-checkstyle  frontend-eslint
mvn checkstyle:check  npm run lint
```

### Docker Compose Services

| Service | Image | Host Port | Purpose |
|---------|-------|-----------|---------|
| `postgres` | `postgres:16` | `5433→5432` | Primary database |
| `redis` | `redis:7-alpine` | `6379→6379` | Cache + pub/sub |
| `elasticsearch` | `elasticsearch:8.11.0` | `9200, 9300` | Full-text search |

```bash
docker compose up -d          # start all
docker compose ps             # status
docker compose logs -f redis  # tail logs
docker compose down           # stop all
```

---

## Development Roadmap

| Sprint | Focus | Status |
|--------|-------|--------|
| 1 | Walking skeleton — JWT auth, STOMP messaging, Flyway, CI | Complete |
| 2 | GitHub OAuth2 integration, webhook ingestion, sync logging | Complete |
| 3 | Stripe billing, subscription tiers, AI extraction (gpt-4o-mini) | In Progress |
| 4 | AI & RAG — vector embeddings, semantic search, documentation assistant | Planned |
| 5 | DevOps — Docker multi-stage images, Kubernetes manifests, observability dashboards | Planned |

---

## License

Proprietary software. All rights reserved.

## Author

**Ankit Kumar** — [@ankit-dev42](https://github.com/ankit-dev42)
