<!-- 
  SYNC IMPACT REPORT
  Version: 1.0.0 (Initial Constitution)
  Ratification Date: 2026-04-12
  Last Amendment: 2026-04-12
  
  PRINCIPLES ADDED:
  - I. Code Quality Excellence
  - II. Testing Standards (Test-First Development)
  - III. User Experience Consistency
  - IV. Performance Requirements
  
  SECTIONS ADDED:
  - Technology Stack Alignment
  - Development Workflow & Review Gates
  
  TEMPLATES UPDATED:
  ✅ plan-template.md - Constitution Check section references 4 core principles
  ✅ spec-template.md - Requirements section aligns with quality and UX principles
  ✅ tasks-template.md - Test and implementation phases organized per constitution
-->

# SyncDoc AI Constitution

**The Non-Negotiable Rules of Engineering**
Version 1.0.0 · April 2026

> AI-Powered Documentation Synchronization Platform
> Spring Boot 3 · Java 17 · React 18 · PostgreSQL · Redis · Stripe · OpenAI

---

## Core Principles

### I. Code Quality Excellence

Code must be written for humans, not machines. Every line of code is a liability until proven valuable.

**Non-Negotiable Rules:**
- All public methods and classes must have clear, descriptive names; if the name requires explanation comment, the name is wrong
- Google Java Style Guide enforced on backend; Prettier enforced on frontend — no manual formatting
- Maximum method length: 40 lines. Maximum class length: 300 lines. Extract if exceeded.
- No magic numbers or strings; define named constants in `application.yml` or code
- Service methods never return `null`; use `Optional<T>` or throw specific exceptions
- Cyclomatic complexity per method must be ≤ 10. Complex logic must be explained via comments or extracted.

**Rationale**: Maintainability compounds. Clean code prevents 80% of production bugs. Every engineer on the team must be able to understand any module within 10 minutes of reading.

---

### II. Testing Standards (Test-First Development)

Testing is not optional; it is the specification. Tests are written and approved **before** implementation begins.

**Non-Negotiable Rules:**
- **TDD Mandatory**: Outline test cases → User approval → Tests fail (Red) → Implement (Green) → Refactor (Refactor) cycle
- Unit test coverage **minimum 70%** for business logic; 100% for critical paths (auth, billing, data sync)
- Integration tests required for: new API endpoints, database schema changes, third-party API integration, async event handlers
- Mock external dependencies (Stripe, OpenAI, GitHub API); use fixtures for deterministic test data
- Backend: JUnit 5 + Mockito. Frontend: React Testing Library with user-centric assertions (no implementation details)
- All tests run in CI/CD; no PR may merge with failing tests or coverage below threshold

**Rationale**: Tests are the only truth. They document behavior, prevent regressions, and enable safe refactoring. A feature without tests is a feature that breaks silently.

---

### III. User Experience Consistency

Every user interaction must feel intentional, predictable, and delightful. Inconsistent UX is a bug.

**Non-Negotiable Rules:**
- Design system enforced via Tailwind CSS utility classes; no custom CSS unless approved by design review
- All user-facing strings managed in i18n files; no hardcoded text in components
- Loading states, error messages, and success confirmations must be present for all async operations
- Keyboard navigation fully supported; WCAG 2.1 AA minimum accessibility compliance
- User flows documented before implementation; multiple paths tested with real users or stakeholders
- Component composition must be idempotent; rendering the same props **must** always produce the same output

**Rationale**: Users remember bad experiences for months. Consistency builds trust. One confusing UX pattern spreads like a virus if not caught.

---

### IV. Performance Requirements

The system must respond to users in milliseconds, not seconds. Performance is a feature, not a bonus.

**Non-Negotiable Rules:**
- API responses must be ≤ 200ms p95 latency (excluding external API calls)
- Frontend bundle size (gzipped) must be ≤ 150KB; lazy-load features over 50KB
- Database queries must execute in ≤ 100ms; add `EXPLAIN ANALYZE` to PR description for new queries
- WebSocket messages delivered within 50ms; use Redis pub/sub for scaling to 10k+ concurrent users
- Images must be optimized (WebP format, responsive sizes); lazy-load below fold
- Core user journeys (login, create project, sync docs) must complete in ≤ 1 second

**Rationale**: Every 100ms of delay costs conversion. Users perceive slow responses as unreliable systems. Performance debt compounds faster than code debt.

---

## Technology Stack Alignment

SyncDoc AI runs on a proven polyglot stack optimized for rapid iteration and reliability:

| Layer | Stack | Rationale |
|---|---|---|
| **Backend** | Spring Boot 3, Java 17, Spring Data JPA | LTS stability, modern Java features, ecosystem maturity |
| **Database** | PostgreSQL 16, Redis 7 | ACID compliance, JSONB for webhooks, pub/sub for scaling |
| **Frontend** | React 18, TypeScript 5, Vite 5, Tailwind CSS 3 | Concurrent rendering, type safety, sub-second HMR |
| **Auth** | Spring Security 6, JWT + OAuth2 | Industry standard, zero-trust by default |
| **Async** | Spring Events, @Async, Spring Scheduler | No external message queue needed for MVP |
| **AI Integration** | Spring AI 1.x with OpenAI | Model-agnostic abstraction layer |
| **Payments** | Stripe SDK | PCI-compliant, no card data on our servers |
| **DevOps** | Docker Compose, GitHub Actions | Reproducible local dev, CI/CD without complexity |

---

## Development Workflow & Review Gates

Every PR must pass these gates before merge:

### Code Review Gate
- Minimum 2 approvals (1 backend engineer, 1 frontend engineer if both affected)
- Reviewer checklist: Constitution compliance, test coverage, performance impact
- ❌ Auto-reject if: no tests, failing CI, constitution violations, undocumented complexity

### Testing Gate
- ✅ All tests pass in CI/CD
- ✅ Coverage reports attached (delta coverage must be ≥ 70%)
- ✅ Integration tests pass against staging database

### Performance Gate
- ✅ Load test results for API changes (if expected traffic > 100 req/s)
- ✅ Bundle size analysis for frontend changes
- ✅ Query plans provided for new database queries

### Documentation Gate
- ✅ Javadoc on all public methods
- ✅ User documentation updated (if customer-facing change)
- ✅ Runbook added for operational changes (deployments, migrations)

---

## Governance

This constitution is the source of truth. All practices, reviews, and deployment decisions must align.

**Amendment Process:**
- Proposal must be documented with rationale and business impact
- Requires unanimous agreement from tech leads (backend, frontend, DevOps)
- Minor wording updates require no approval (PATCH version bump)
- New principles or removals require sprint planning cycle (MINOR version bump)
- Incompatible principle changes require feature deprecation (MAJOR version bump)

**Compliance Verification:**
- PR reviews must cite affected principles (e.g., "Violates Principle II: Test-First Development")
- Sprint retros include constitution compliance audit
- Code quality metrics (coverage, latency, bundle size) dashboarded weekly

**Living Document:**
- Constitution is reviewed quarterly or when pain points emerge
- Runtime guidance lives in [`README.md`](README.md) and [docs/](docs/) — constitution never changes without architectural impact
- All engineers must read this document before their first commit

---

**Version**: 1.0.0 | **Ratified**: 2026-04-12 | **Last Amended**: 2026-04-12
