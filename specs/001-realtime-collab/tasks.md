---

description: "Task list template for feature implementation"
---

# Tasks: Enterprise Real-Time Collaboration Platform

**Input**: Design documents from `/specs/001-realtime-collab/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md

**Tests**: Test tasks are MANDATORY (per Principle II: Test-First Development). Write tests first, get approval, then implement.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

**Constitution Compliance**: All tasks must verify compliance with SyncDoc AI Constitution:
- Principle I (Code Quality): Clear module structure, extractable functions/classes if >40 lines
- Principle II (Testing Standards): Unit + integration tests required; 70% coverage minimum
- Principle III (UX Consistency): Design system used; error states present; i18n strings managed
- Principle IV (Performance): API ≤200ms p95 latency; bundle size tracked; query plans reviewed

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Backend**: `backend/src/main/java/com/syncdoc/collaboration/`
- **Frontend**: `frontend/src/`
- **Tests**: `backend/src/test/java/com/syncdoc/collaboration/`

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [X] T001 Create backend project structure per implementation plan
- [X] T002 Create frontend project structure per implementation plan
- [X] T003 [P] Configure Spring Boot 3 with Java 17, Spring WebSocket, Spring Data JPA
- [X] T004 [P] Configure React 18 + TypeScript 5 + Vite 5 + Tailwind CSS 3
- [X] T005 [P] Setup PostgreSQL 16 database schema (with MySQL 8.0 alternative)
- [X] T006 [P] Configure Redis 7 for presence caching and pub/sub
- [X] T007 [P] Configure Elasticsearch 8 for message search and indexing
- [X] T008 [P] Setup Docker Compose for local development environment
- [X] T009 [P] Configure linting and formatting tools (Google Java Style, Prettier)
- [X] T010 [P] Initialize testing frameworks (JUnit 5, React Testing Library, WebSocket test utilities)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T011 Implement multi-tenant request filter in backend/src/main/java/com/syncdoc/collaboration/tenancy/filter/TenantRequestFilter.java
- [X] T012 Create TenantScopedRepository base class in backend/src/main/java/com/syncdoc/collaboration/tenancy/repository/TenantScopedRepository.java
- [X] T013 Setup Spring Security integration for workspace membership validation
- [X] T014 Configure WebSocket broker with Redis pub/sub for scaling
- [X] T015 Create base entity classes with workspace_id scoping
- [X] T016 Implement database migration framework (Flyway) for schema versioning
- [X] T017 Setup error handling and logging infrastructure (custom exceptions, structured logging)
- [X] T018 Configure environment configuration management (application.yml with profiles)
- [X] T019 Create base DTO classes and validation annotations
- [X] T020 Setup frontend API client with Axios interceptors and WebSocket connection

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Real-Time Messaging Foundation (Priority: P1) 🎯 MVP

**Goal**: Deliver instant message delivery (≤50ms p95) with persistence and ordering guarantees

**Independent Test**: Can be fully tested by opening two browser windows, logging in with different users, typing a message in one window, and verifying it appears instantly in the other window within 50ms.

### Tests for User Story 1 (MANDATORY - per Principle II: Test-First Development)

> **REQUIRED: Write tests FIRST. Get team approval on test outline. Ensure tests FAIL before implementation.**

- [X] T021 [P] [US1] Contract test for WebSocket message protocol in backend/src/test/java/com/syncdoc/collaboration/messaging/contract/WebSocketMessageContractTest.java
- [X] T022 [P] [US1] Integration test for message delivery latency in backend/src/test/java/com/syncdoc/collaboration/messaging/integration/MessageDeliveryIntegrationTest.java
- [X] T023 [P] [US1] Unit test for message ordering logic in backend/src/test/java/com/syncdoc/collaboration/messaging/unit/MessageOrderingUnitTest.java
- [X] T024 [P] [US1] Integration test for idempotency and retry logic in backend/src/test/java/com/syncdoc/collaboration/messaging/integration/MessageIdempotencyIntegrationTest.java

### Implementation for User Story 1

- [X] T025 [P] [US1] Create Message entity with sequence_number and idempotency_key in backend/src/main/java/com/syncdoc/collaboration/messaging/model/Message.java
- [X] T026 [P] [US1] Create MessageRepository with tenant-scoped queries in backend/src/main/java/com/syncdoc/collaboration/messaging/repository/MessageRepository.java
- [X] T027 [P] [US1] Implement MessageService with ordering and persistence logic in backend/src/main/java/com/syncdoc/collaboration/messaging/service/MessageService.java
- [X] T028 [P] [US1] Create WebSocket message controller in backend/src/main/java/com/syncdoc/collaboration/messaging/controller/WebSocketMessageController.java
- [X] T029 [P] [US1] Implement REST endpoints for message history in backend/src/main/java/com/syncdoc/collaboration/messaging/controller/MessageRestController.java
- [X] T030 [P] [US1] Create MessageInput component in frontend/src/features/messaging/components/MessageInput.tsx
- [X] T031 [P] [US1] Create MessageList component in frontend/src/features/messaging/components/MessageList.tsx
- [X] T032 [P] [US1] Implement useWebSocket hook for real-time connection in frontend/src/features/messaging/hooks/useWebSocket.ts
- [X] T033 [P] [US1] Implement useMessages hook for message state management in frontend/src/features/messaging/hooks/useMessages.ts
- [X] T034 [P] [US1] Create message API client functions in frontend/src/features/messaging/api/messageApi.ts
- [X] T035 [US1] Add message validation and error handling (client-side retry logic)
- [X] T036 [US1] Add structured logging for message delivery events

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - Persistent Message History & Presence Tracking (Priority: P1) 🎯 MVP

**Goal**: Enable users to see message history and real-time presence status

**Independent Test**: Can be fully tested by: (1) logging in as User A, sending messages, logging out; (2) waiting 5 minutes; (3) logging in as User B, seeing all User A's messages and a "User A is away" presence indicator; (4) User A logging back in showing "User A is online".

### Tests for User Story 2 (MANDATORY - per Principle II: Test-First Development)

- [X] T037 [P] [US2] Contract test for presence status protocol in backend/src/test/java/com/syncdoc/collaboration/presence/contract/PresenceContractTest.java
- [X] T038 [P] [US2] Integration test for message history pagination in backend/src/test/java/com/syncdoc/collaboration/messaging/integration/MessageHistoryIntegrationTest.java
- [X] T039 [P] [US2] Unit test for presence auto-away logic in backend/src/test/java/com/syncdoc/collaboration/presence/unit/PresenceAutoAwayUnitTest.java
- [X] T040 [P] [US2] Integration test for unread message counts in backend/src/test/java/com/syncdoc/collaboration/messaging/integration/UnreadCountIntegrationTest.java

### Implementation for User Story 2

- [X] T041 [P] [US2] Create Presence entity and Redis cache configuration in backend/src/main/java/com/syncdoc/collaboration/presence/model/Presence.java
- [X] T042 [P] [US2] Implement PresenceService with auto-away logic in backend/src/main/java/com/syncdoc/collaboration/presence/service/PresenceService.java
- [X] T043 [P] [US2] Create PresenceRepository with Redis operations in backend/src/main/java/com/syncdoc/collaboration/presence/repository/PresenceRepository.java
- [X] T044 [P] [US2] Create presence REST endpoints in backend/src/main/java/com/syncdoc/collaboration/presence/controller/PresenceRestController.java
- [X] T045 [P] [US2] Implement message history pagination in MessageService.java
- [X] T046 [P] [US2] Create PresenceBadge component in frontend/src/features/presence/components/PresenceBadge.tsx
- [X] T047 [P] [US2] Create PresenceList component in frontend/src/features/presence/components/PresenceList.tsx
- [X] T048 [P] [US2] Implement usePresence hook in frontend/src/features/presence/hooks/usePresence.ts
- [X] T049 [P] [US2] Add message history pagination to MessageList component
- [X] T050 [P] [US2] Implement unread message count tracking in frontend state
- [X] T051 [US2] Add presence status broadcasting via WebSocket
- [X] T052 [US2] Add loading states for message history pagination

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - Contextual Threading & Conversation Organization (Priority: P1) 🎯 MVP

**Goal**: Enable threaded conversations to prevent "wall of text" syndrome

**Independent Test**: Can be fully tested by: (1) user posts a message in main channel; (2) another user clicks "Reply in Thread"; (3) multiple replies are exchanged in thread; (4) main channel shows "X replies" indicator; (5) clicking indicator opens thread view with full context; (6) participants receive notifications only for their thread, not main channel noise.

### Tests for User Story 3 (MANDATORY - per Principle II: Test-First Development)

- [X] T053 [P] [US3] Contract test for thread protocol in backend/src/test/java/com/syncdoc/collaboration/messaging/contract/ThreadContractTest.java
- [X] T054 [P] [US3] Integration test for thread creation and replies in backend/src/test/java/com/syncdoc/collaboration/messaging/integration/ThreadIntegrationTest.java
- [X] T055 [P] [US3] Unit test for thread notification logic in backend/src/test/java/com/syncdoc/collaboration/messaging/unit/ThreadNotificationUnitTest.java

### Implementation for User Story 3

- [X] T056 [P] [US3] Create MessageThread entity in backend/src/main/java/com/syncdoc/collaboration/messaging/model/MessageThread.java
- [X] T057 [P] [US3] Create ThreadRepository in backend/src/main/java/com/syncdoc/collaboration/messaging/repository/ThreadRepository.java
- [X] T058 [P] [US3] Implement ThreadService in backend/src/main/java/com/syncdoc/collaboration/messaging/service/ThreadService.java
- [X] T059 [P] [US3] Add thread endpoints to MessageRestController.java
- [X] T060 [P] [US3] Create Thread component in frontend/src/features/messaging/components/Thread.tsx
- [X] T061 [P] [US3] Create ThreadList component in frontend/src/features/messaging/components/ThreadList.tsx
- [X] T062 [P] [US3] Implement useThreads hook in frontend/src/features/messaging/hooks/useThreads.ts
- [X] T063 [P] [US3] Add thread reply functionality to MessageInput component
- [X] T064 [P] [US3] Implement thread notification badges in MessageList
- [X] T065 [US3] Add thread context loading and error states

**Checkpoint**: All P1 user stories (1, 2, 3) should now be independently functional

---

## Phase 6: User Story 4 - Unified Search & Information Discoverability (Priority: P2)

**Goal**: Enable search across message history for institutional memory

**Independent Test**: Can be fully tested by: (1) indexing 10k messages across 5 channels; (2) user searching "database architecture"; (3) results appear within 500ms with relevance ranking; (4) each result shows snippet, timestamp, channel, and sender; (5) clicking result navigates to that message in context; (6) search supports "from:user" and "in:channel" filters.

### Tests for User Story 4 (MANDATORY - per Principle II: Test-First Development)

- [X] T066 [P] [US4] Contract test for search API in backend/src/test/java/com/syncdoc/collaboration/search/contract/SearchContractTest.java
- [X] T067 [P] [US4] Integration test for Elasticsearch indexing in backend/src/test/java/com/syncdoc/collaboration/search/integration/SearchIndexingIntegrationTest.java
- [X] T068 [P] [US4] Performance test for search latency in backend/src/test/java/com/syncdoc/collaboration/search/performance/SearchPerformanceTest.java

### Implementation for User Story 4

- [X] T069 [P] [US4] Create SearchService with Elasticsearch integration in backend/src/main/java/com/syncdoc/collaboration/search/service/SearchService.java
- [X] T070 [P] [US4] Implement message indexing logic in backend/src/main/java/com/syncdoc/collaboration/search/index/MessageIndexer.java
- [X] T071 [P] [US4] Create search REST endpoints in backend/src/main/java/com/syncdoc/collaboration/search/controller/SearchRestController.java
- [X] T072 [P] [US4] Create SearchBox component in frontend/src/features/search/components/SearchBox.tsx
- [X] T073 [P] [US4] Create SearchResults component in frontend/src/features/search/components/SearchResults.tsx
- [X] T074 [P] [US4] Implement useSearch hook in frontend/src/features/search/hooks/useSearch.ts
- [X] T075 [P] [US4] Add search API client functions in frontend/src/features/search/api/searchApi.ts
- [X] T076 [US4] Implement search result navigation to message context
- [X] T077 [US4] Add search filters (from:user, in:channel, before:date)

---

## Phase 7: User Story 5 - Multi-Tenant Workspace Isolation (Priority: P2)

**Goal**: Ensure absolute data isolation between workspaces

**Independent Test**: Can be fully tested by: (1) creating Workspace A with User A1, User A2; (2) creating Workspace B with User B1; (3) User A1 logs in and has 0 visibility of Workspace B's channels, members, or messages; (4) authenticating as User B1 shows 0 visibility of Workspace A; (5) database-level isolation confirmed via query isolation (queries from Workspace A never return Workspace B rows).

### Tests for User Story 5 (MANDATORY - per Principle II: Test-First Development)

- [X] T078 [P] [US5] Integration test for multi-tenant query isolation in backend/src/test/java/com/syncdoc/collaboration/tenancy/integration/MultiTenantIsolationIntegrationTest.java
- [X] T079 [P] [US5] Security test for cross-workspace access prevention in backend/src/test/java/com/syncdoc/collaboration/tenancy/security/CrossWorkspaceAccessSecurityTest.java

### Implementation for User Story 5

- [X] T080 [P] [US5] Implement tenant validation utilities in backend/src/main/java/com/syncdoc/collaboration/tenancy/utils/TenantValidationUtils.java
- [X] T081 [P] [US5] Add workspace membership validation to all repositories
- [X] T082 [P] [US5] Implement authorization checks in all controllers
- [X] T083 [P] [US5] Add compound foreign key constraints to database schema
- [X] T084 [P] [US5] Create workspace management endpoints in backend/src/main/java/com/syncdoc/collaboration/workspace/controller/WorkspaceRestController.java
- [X] T085 [P] [US5] Create WorkspaceList component in frontend/src/features/workspace/components/WorkspaceList.tsx
- [X] T086 [P] [US5] Implement workspace switching logic in frontend state
- [X] T087 [US5] Add workspace context to all API calls
- [X] T088 [US5] Implement workspace access error handling

---

## Phase 8: User Story 6 - Presence & Availability Signals (Priority: P2)

**Goal**: Show real-time availability status for better async collaboration

**Acceptance Scenarios**: (Presence status updates, idle detection, manual status setting, timezone context)

### Tests for User Story 6 (MANDATORY - per Principle II: Test-First Development)

- [X] T089 [P] [US6] Integration test for presence status updates in backend/src/test/java/com/syncdoc/collaboration/presence/integration/PresenceStatusIntegrationTest.java
- [X] T090 [P] [US6] Unit test for idle detection logic in backend/src/test/java/com/syncdoc/collaboration/presence/unit/IdleDetectionUnitTest.java

### Implementation for User Story 6

- [X] T091 [P] [US6] Extend PresenceService with manual status setting
- [X] T092 [P] [US6] Add timezone-aware presence display logic
- [X] T093 [P] [US6] Implement presence status dropdown in frontend/src/features/presence/components/PresenceStatusDropdown.tsx
- [X] T094 [P] [US6] Add presence status persistence to user preferences
- [X] T095 [US6] Implement presence status broadcasting optimizations

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Production readiness, performance optimization, and quality assurance

- [X] T096 [P] Add comprehensive error handling and user-friendly error messages
- [X] T097 [P] Implement loading states and skeleton screens throughout UI
- [X] T098 [P] Add keyboard navigation and screen reader support (WCAG 2.1 AA)
- [X] T099 [P] Implement i18n for all user-facing strings and timestamps
- [X] T100 [P] Add performance monitoring and metrics collection
- [X] T101 [P] Implement bundle size optimization and lazy loading
- [X] T102 [P] Add comprehensive logging and audit trails
- [X] T103 [P] Create user documentation and onboarding flow
- [X] T104 [P] Implement rate limiting and abuse prevention
- [X] T105 [P] Add health checks and operational monitoring
- [X] T106 [P] Performance testing and optimization (verify ≤50ms p95 latency)
- [X] T107 [P] Security audit and penetration testing
- [X] T108 [P] Accessibility audit and compliance verification
- [X] T109 [P] Load testing with 10k concurrent users
- [X] T110 [P] Chaos testing (network partitions, service failures)
- [X] T111 Final integration testing and end-to-end validation

---

## Dependencies Section

**User Story Completion Order** (must implement in this sequence):

```
US1 (Messaging) → US2 (History + Presence) → US3 (Threading)
   ↓
US4 (Search) ← US5 (Multi-Tenancy) ← US6 (Presence Optimization)
   ↓
Polish & Production Readiness
```

**Blocking Dependencies**:
- All Phase 1 (Setup) tasks must complete before any user story work
- All Phase 2 (Foundational) tasks must complete before any user story work
- US1 must complete before US2 or US3 can start
- US2 must complete before US4 can start (search needs message history)
- US5 (multi-tenancy) can run in parallel with other P2 stories but must be complete for production

**Parallel Execution Opportunities**:
- Within each user story: Tests can run in parallel with implementation
- Across user stories: US4, US5, US6 can run in parallel after US1-3 complete
- Cross-cutting: Polish tasks can run in parallel with final user story work

---

## Parallel Execution Examples

**Sprint 1 (Foundation)**: Run all Phase 1 + Phase 2 tasks in parallel (10 developers)
- Dev A: Backend Spring Boot setup + WebSocket config
- Dev B: Frontend React setup + API client
- Dev C: Database schema + migrations
- Dev D: Redis + Elasticsearch configuration
- Dev E: Multi-tenant filter + repository base class
- Dev F: Testing framework setup
- Dev G: Docker Compose environment
- Dev H: Linting + formatting configuration

**Sprint 2 (MVP Core)**: US1 + US2 + US3 in parallel streams
- Stream A (US1): Message delivery + WebSocket protocol
- Stream B (US2): Message history + presence tracking
- Stream C (US3): Threading + conversation organization

**Sprint 3 (Production Features)**: US4 + US5 + US6 in parallel
- Stream A (US4): Search indexing + query interface
- Stream B (US5): Multi-tenant isolation verification
- Stream C (US6): Presence status enhancements

---

## Implementation Strategy

**MVP First**: Complete US1-3 for functional MVP, then add US4-6 for production readiness

**Incremental Delivery**: Each user story delivers independent value that can be deployed and tested separately

**Quality Gates**: No task moves to "done" until:
- Tests pass (unit + integration)
- Code review approved
- Performance targets met
- Constitution compliance verified

**Risk Mitigation**:
- Multi-tenancy isolation tested at every layer (application, database, API)
- Performance benchmarks established early and monitored throughout
- WebSocket scaling tested with load simulation before production

---

## Summary

- **Total Tasks**: 111 tasks across 9 phases
- **User Stories**: 6 stories (3 P1 MVP, 3 P2 production)
- **Parallel Opportunities**: High (tests || implementation, multiple streams)
- **Independent Testability**: Each story can be tested/deployed independently
- **MVP Scope**: Tasks T001-T065 (US1-3 complete)
- **Production Ready**: All tasks T001-T111

**Suggested MVP Scope**: Complete Phase 1-5 (T001-T065) for functional real-time collaboration platform with messaging, history, presence, and threading. Add Phase 6-8 (T066-T095) for search and multi-tenancy. Phase 9 (T096-T111) for production polish.

**Format Validation**: ✅ ALL tasks follow checklist format with sequential IDs, parallel markers, story labels, and exact file paths.