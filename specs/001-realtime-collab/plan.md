# Implementation Plan: Enterprise Real-Time Collaboration Platform

**Branch**: `001-realtime-collab` | **Date**: 2026-04-12 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from [spec.md](spec.md)

## Summary

Build a high-performance, real-time communication platform serving as the "collaboration nervous system" for distributed enterprises. Core MVP delivers instant message delivery (≤50ms p95), persistent history, contextual threading, and strict multi-tenant isolation. Scales horizontally from 10-person teams to global organizations. P1 stories (messaging, history, threading) establish foundation; P2 stories (search, tenancy guardrails, presence optimization) enable production SaaS operations.

## Technical Context

**Language/Version**: Java 17 (LTS) for backend; TypeScript 5 for frontend  
**Primary Dependencies**: Spring Boot 3, Spring WebSocket, Spring Data JPA, React 18, Vite 5, Tailwind CSS 3  
**Storage**: **PostgreSQL 16 or MySQL 8.0** (see Decision section below), Redis 7 (presence/cache), Elasticsearch 8 (search/archive)  
**Testing**: JUnit 5 + Mockito (backend unit/integration), React Testing Library (frontend), WebSocket load tests  
**Target Platform**: Enterprise SaaS web service + browser-based SPA (Chrome/Safari/Edge)  
**Project Type**: Web service (REST API + WebSocket) + React SPA  
**Performance Goals**: Message delivery ≤50ms p95, presence updates ≤100ms, search ≤500ms across 100M+ messages, 10k concurrent users per workspace  
**Constraints**: API latency ≤200ms (excluding external calls), frontend bundle ≤150KB gzipped, zero message loss, 99.99% uptime SLA  
**Scale/Scope**: Multi-tenant SaaS supporting 10-5000 person workspaces, 100M+ message archive, 1000+ concurrent channels per workspace

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**For this feature, verify compliance with:**

- ✅ **Principle I (Code Quality)**: Module structure organized by feature (messaging, presence, search, tenancy). WebSocket handlers, message ordering logic, and multi-tenancy isolation are complex—will be extracted into Named strategies with clear interfaces. All public methods have clear names; retry/idempotency logic documented.
- ✅ **Principle II (Testing Standards)**: Test cases outlined in spec (26 acceptance scenarios). TDD: contract tests for WebSocket protocol, unit tests for ordering/idempotency (≥90% coverage), integration tests for multi-tenancy isolation, load tests (10k concurrent users). No code written until tests approved.
- ✅ **Principle III (UX Consistency)**: Loading states for message history pagination, typing indicators, presence badges. Error messages ("message send failed – retrying"). i18n for timestamps (locale-aware), notifications, status strings. Keyboard nav (Ctrl+K channel search, Tab navigation). WCAG 2.1 AA for screen readers.
- ✅ **Principle IV (Performance)**: Latency targets explicit (≤50ms message, ≤100ms presence, ≤500ms search). Bundle size target ≤150KB gzipped. Database query plans reviewed (all ≤100ms). WebSocket frame optimization (compression enabled, ≤64KB per message).

**✅ NO GATE VIOLATIONS** — Feature aligns fully with constitution. Performance and testing requirements explicitly aligned.

## Project Structure

### Documentation (this feature)

```text
specs/001-realtime-collab/
├── plan.md              # This file
├── spec.md              # Feature specification (user stories, requirements)
├── research.md          # Phase 0 output (technology research, alternatives evaluated)
├── data-model.md        # Phase 1 output (entity definitions, relationships, state machines)
├── contracts/           # Phase 1 output (WebSocket protocol, API schemas, multi-tenant isolation rules)
│   ├── websocket-protocol.md
│   ├── rest-endpoints.yml
│   ├── database-schema-isolation.md
│   └── multi-tenant-queries.sql
├── quickstart.md        # Phase 1 output (setup guide, local dev environment, key workflows)
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

**Structure**: Web application (backend + frontend SPA)

```text
backend/
├── src/main/java/com/syncdoc/collaboration/
│   ├── messaging/
│   │   ├── controller/       # WebSocket handlers, REST endpoints
│   │   ├── service/          # Message persistence, ordering, retry logic
│   │   ├── repository/       # JPA repositories (workspace-scoped queries)
│   │   └── model/            # Message entity, DTO
│   ├── presence/
│   │   ├── controller/       # Presence status endpoints
│   │   ├── service/          # Presence tracking, auto-away logic
│   │   ├── cache/            # Redis presence backend
│   │   └── model/
│   ├── search/
│   │   ├── service/          # Elasticsearch integration
│   │   ├── index/            # Message indexing, re-indexing
│   │   └── query/            # Search filtering
│   ├── tenancy/
│   │   ├── filter/           # multi-tenant request filter (workspace isolation)
│   │   ├── repository/       # TenantScoped base for all repos
│   │   └── utils/            # Current tenant context, validation
│   ├── auth/                 # Inherited from SyncDoc AI (no new auth)
│   ├── config/               # Spring config (WebSocket, datasource, Redis, Elasticsearch)
│   └── exception/            # Custom exceptions (MultiTenancyViolation, MessageOrderingError)
├── src/test/java/com/syncdoc/collaboration/
│   ├── messaging/
│   │   ├── contract/         # WebSocket protocol contract tests
│   │   ├── integration/       # Message ordering, idempotency tests
│   │   └── unit/             # Service unit tests
│   ├── tenancy/
│   │   └── integration/       # Multi-tenant isolation tests (query-level verification)
│   ├── load/                 # Load tests (10k concurrent users, 100k msg/sec)
│   └── chaos/                # Chaos tests (network partition, clock skew)
└── pom.xml

frontend/
├── src/
│   ├── features/
│   │   ├── messaging/
│   │   │   ├── components/   # MessageList, MessageInput, Thread
│   │   │   ├── hooks/        # useMessages, useWebSocket, useThreads
│   │   │   └── api/          # API calls (sendMessage, fetchHistory, subscribePresence)
│   │   ├── presence/
│   │   │   ├── components/   # PresenceBadge, PresenceList
│   │   │   ├── hooks/        # usePresence
│   │   │   └── api/
│   │   ├── search/
│   │   │   ├── components/   # SearchBox, SearchResults
│   │   │   ├── hooks/        # useSearch
│   │   │   └── api/
│   │   └── workspace/        # Workspace list, member management
│   ├── components/           # Shared UI (buttons, modals, dropdowns per design system)
│   ├── hooks/                # useAuth (inherited), useWebSocket (new), useLocalStorage
│   ├── store/                # Zustand stores (auth, workspace, UI state)
│   ├── api/                  # Axios instance, interceptors, WebSocket client
│   ├── types/                # TypeScript interfaces
│   ├── utils/                # Helpers (timestamp formatting, i18n, idempotency)
│   └── i18n/                 # Locale strings (en, es, fr, de)
├── tests/
│   ├── contract/             # WebSocket protocol tests
│   ├── integration/          # Multi-step user journeys
│   ├── unit/                 # Component and hook tests
│   └── performance/          # Bundle size, load testing
└── package.json
```

**Structure Decision**: Web application (both backend and frontend) because feature spans both layers. Backend handles core real-time logic (message ordering, multi-tenancy isolation, presence tracking). Frontend handles UX (WebSocket connection, typing indicators, thread panel). Both must be implemented together for feature to function.

---

## Database Technology Decision: PostgreSQL vs MySQL

**User Request**: "Keep options for both PostgreSQL and MySQL"

### Decision: PostgreSQL 16 (Primary), MySQL 8.0 (Alternative)

| Criterion | PostgreSQL 16 | MySQL 8.0 | SyncDoc Default |
|---|---|---|---|
| **ACID Compliance** | ✅ Full MVCC, true isolation levels | ✅ InnoDB full ACID | PostgreSQL preferred |
| **JSON Support** | ✅ JSONB (indexed, queryable) | ⚠️ JSON (not indexed) | PostgreSQL advantage |
| **Full-Text Search** | ✅ Native, integrated | ⚠️ MyISAM only (deprecated) | PostgreSQL advantage |
| **Scaling (Read Replicas)** | ✅ WAL streaming, logical replication | ✅ Binlog replication | Both capable |
| **Performance (100M+ messages)** | ✅ Index on (workspace_id, created_at) | ✅ Composite indexes | Both capable|
| **Operational Simplicity** | ✅ Docker Compose, pg_dump | ✅ Docker Compose, mysqldump | Equal |
| **SyncDoc AI Alignment** | ✅ **Chosen for SyncDoc** | ⚠️ Alternative | **PostgreSQL 16** |

### Recommendation

**Use PostgreSQL 16** (aligns with SyncDoc AI constitution stack). MySQL 8.0 is viable alternative if organization has stronger MySQL expertise. Critical requirement: all queries must make multi-tenant isolation explicit (see contracts/database-schema-isolation.md).

**Implementation Decision**: 
- Start with PostgreSQL 16 per constitution
- Abstraction layer (Spring Data JPA repository pattern) allows MySQL swap-out in future if needed
- No raw SQL in application code—queries always go through repository layer
- All repositories inherit `TenantScopedRepository` base class that auto-applies workspace_id filter

---

## Complexity Tracking

> No constitution violations. Feature aligns fully with Code Quality, Testing, UX, and Performance principles.

| Area | Complexity Factor | Justification | Mitigation |
|---|---|---|---|
| **WebSocket at Scale** | High | Managing 10k concurrent connections. Client reconnect storms, frame ordering, idempotency. | Load tests (chaos scenarios). Named strategies (MessageDeduplicator, RetryPolicy). |
| **Multi-Tenant Data Isolation** | High (Critical) | Must be 100% bulletproof. Query-level leakage unacceptable. Requires database-level foreign keys + application filter. | TenantScopedRepository base class (all queries filtered). Integration tests verify 0% leakage. Code reviews focus on isolation. |
| **Message Ordering Guarantees** | High | Clock skew, multi-region later. Server-side timestamps are source of truth. Ordering is customer-visible (no "reordered messages" allowed). | Tests verify ordering with concurrent sends + network delays. Database constraints prevent duplicates. |
| **Search at Scale** | High | Elasticsearch indexing 100M+ messages. Index freshness, deletion handling, pagination. | Async indexing (message queued → indexed). Soft deletes (messages marked deleted, excluded from search). |

**No approval needed for complexity**: All factors are explained in spec and mitigated by design.

---

## Phase 0: Research & Technology Validation

**Status**: Ready to execute  
**Output**: [research.md](research.md)

### Research Tasks

The following technical unknowns and alternatives will be researched and documented:

1. **WebSocket Scaling Patterns**  
   *Unknown*: How to handle 10k concurrent connections at ≤50ms latency without external message broker?  
   *Decision*: Spring WebSocket with Tomcat NIO connectors vs. Netty vs. Vert.x  
   *Research*: Benchmark all three with 10k concurrent connections + 100k msg/sec throughput. Document max connection limits, latency degradation curves, memory footprint.

2. **Message Ordering at Scale**  
   *Unknown*: How to guarantee message order when clients send simultaneously and network clock skew exists?  
   *Decision*: Server-assigned sequence number vs. Lamport timestamp vs. NTP-synchronized client clocks  
   *Research*: Compare approaches for correctness, operational complexity, and client-side burden. Test with 1ms clock skew scenarios.

3. **Search Infrastructure Options**  
   *Unknown*: Elasticsearch vs. PostgreSQL full-text vs. Meilisearch for 100M+ message archive  
   *Decision*: Elasticsearch (current plan) vs. PostgreSQL tsvector (simpler, no external service)  
   *Research*: Benchmark indexing latency, query performance (p95 ≤500ms), memory overhead for each. Evaluate operational complexity (backup, scaling, monitoring).

4. **Multi-Tenancy Implementation Patterns**  
   *Unknown*: Row-level security vs. application-layer filtering vs. schema-per-tenant  
   *Decision*: Application-layer filtering (TenantScopedRepository) vs. PostgreSQL RLS (role-based)  
   *Research*: Document security/performance/operational tradeoffs. Verify zero query-level leakage scenarios.

5. **Redis Pub/Sub vs. WebSocket for Presence**  
   *Unknown*: How to broadcast presence updates to 10k+ connected clients efficiently?  
   *Decision*: Redis pub/sub + Spring WebSocket vs. simple in-memory Map vs. Kafka topics  
   *Research*: Benchmark latency (target ≤100ms), memory usage per connection, failover scenarios.

6. **Message Deduplication Strategy**  
   *Unknown*: How to prevent duplicates when clients retry failed sends (exactly-once semantics)?  
   *Decision*: Idempotency keys (client UUID + seq number) vs. distributed transaction vs. database unique constraint  
   *Research*: Test with flaky network (simulate 50% message loss rate) and verify exactly-once on receipts. Document latency impact.

### Phase 0 Expected Outcomes

Research.md will contain:
- Decision: selected technology + rationale
- Alternatives considered: evaluated options + why rejected
- Risk assessment: known limitations, mitigations
- Operational notes: setup, scaling limits, monitoring
- All unknowns resolved (no "NEEDS CLARIFICATION" remain)

---

## Phase 1: Design & Architecture

**Status**: Blocked on Phase 0 completion  
**Outputs**: data-model.md, contracts/, quickstart.md (and update to plan.md Constitution Check)

### 1.1: Data Model Design

**Input**: 7 entities from spec.md (Workspace, Member, Channel, Message, MessageThread, Presence, MessageReaction)

**Deliverable**: [data-model.md](data-model.md) with:
- Entity definitions (fields, types, constraints, indexes)
- Relationships (foreign keys, multi-tenancy scoping)
- State machines (message states: pending → persisted → delivered; presence states: online → away → offline)
- Validation rules (message size ≤10MB, workspace_id never null, etc.)
- Database constraints (unique constraints for idempotency, check constraints for valid states)
- Performance indexes: (workspace_id, created_at) on messages, (workspace_id, channel_id) on threads

### 1.2: Interface Contracts

**Input**: 6 user stories with acceptance scenarios (26 total test cases)

**Deliverable**: [contracts/](contracts/) folder with:

- **websocket-protocol.md**: Define WebSocket frame protocol (message, presence update, typing indicator, error frames). Include schema for each frame type.
- **rest-endpoints.yml**: OpenAPI 3.0 spec for HTTP endpoints (list channels, fetch history pagination, search, presence endpoints). Include error codes (400/403/409/500).
- **database-schema-isolation.md**: Explicit documentation of multi-tenancy isolation rules (every query must include workspace_id in WHERE clause, no exception). Include verification steps.
- **multi-tenant-queries.sql**: Example queries annotated with isolation verification (✅ or ❌ multi-tenant safe).

### 1.3: Quickstart & Local Development

**Deliverable**: [quickstart.md](quickstart.md) with:
- Docker Compose setup (PostgreSQL, Redis, Elasticsearch)
- Running backend: `mvn spring-boot:run` (backend starts on port 8080, WebSocket on ws://localhost:8080/ws)
- Running frontend: `npm run dev` (Vite dev server on port 5173)
- Creating test data: script to create workspace, users, channels, messages
- Sanity checks: curl examples to verify messaging, presence, search work end-to-end
- Load testing: k6 script to test 100 concurrent users, 1000 msg/sec
- Troubleshooting: common issues (WebSocket connection fails, presence not updating, search empty) + solutions

### 1.4: Update Agent Context

**Execution**: Run `.specify/scripts/bash/update-agent-context.sh copilot`
- Updates Copilot context file with Spring WebSocket, Elasticsearch, Redis, React patterns
- Preserves existing SyncDoc AI context (Spring Boot, PostgreSQL, React basics)
- Adds only new technologies introduced by this feature

### 1.5: Re-Evaluate Constitution Check

**Gate**: Verify Phase 1 artifacts don't introduce new violations:
- ✅ Principle I: Module structure clearly shows isolation (messaging/tenancy/search as separate modules)
- ✅ Principle II: Contract tests (WebSocket, REST, isolation) are defined and ready for TDD
- ✅ Principle III: UX requirements documented (loading states, error messages, i18n prep)
- ✅ Principle IV: Performance targets reflected in data model (indexes optimized, query plans reviewed)

---

## Next Steps

1. **Execute Phase 0**: Research technologies; document findings in research.md
2. **Execute Phase 1**: Design data model, contracts, quickstart
3. **Update Agent Context**: Run script to sync new technologies with Copilot documentation
4. **Proceed to /speckit.tasks**: Generate task plan for implementation

---

**Plan Status**: ✅ READY FOR PHASE 0 EXECUTION  
**Last Updated**: 2026-04-12  
**Next Review**: After Phase 0 research completion
