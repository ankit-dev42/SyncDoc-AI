# Research: Phase 2 — Business Logic

**Generated**: 2026-04-20  
**Purpose**: Resolve all technical unknowns identified during Technical Context analysis  
**Status**: Complete — no NEEDS CLARIFICATION items remain

---

## R-001 — Stripe SDK Integration with Spring Boot 3

**Decision**: `com.stripe:stripe-java:23.x` as a singleton `@Bean` that initialises
`Stripe.apiKey` once at construction time by reading `${integrations.stripe.secret-key}`.

**Rationale**:
- `Stripe.apiKey` is a static field on the `com.stripe.Stripe` class. Setting it once
  in a `@Bean` constructor is the idiomatic way to initialise the SDK in Spring;
  there is no Spring Boot auto-configuration provided by the Stripe library itself.
- Wrapping the three SDK operations (`fetchSubscription`, `createCheckoutSession`,
  `createPortalSession`) behind the existing `StripeClient` interface keeps them
  mockable in tests without touching the SDK.
- `Webhook.constructEvent(rawBody, sigHeader, webhookSecret)` is the correct
  verification API. It both parses the payload AND verifies the HMAC-SHA-256
  signature atomically — do not split these operations.
- Idempotency: check `processedStripeEventRepository.existsByStripeEventId(id)`
  **before** any mutation. Return 200 immediately on duplicate.

**Webhook signature verification note (Constitution Rule 02)**:
```java
Event event = Webhook.constructEvent(rawBody, sigHeader, webhookSecret);
// Only code reaching this line has a valid signature
```
`constructEvent` throws `SignatureVerificationException` on invalid signatures;
map this to a 400 response before any further processing.

**Alternatives Considered**:
- Spring Boot OAuth2 resource server: not applicable — Stripe is not an IdP.
- Manually implementing HMAC-SHA-256: duplicates SDK logic and is error-prone.

---

## R-002 — Spring AI 1.x ChatClient for OpenAI

**Decision**: `spring-ai-openai-spring-boot-starter` (version managed by `spring-ai-bom:1.0.0-SNAPSHOT`
or the released `1.0.0` once available). Use the auto-configured `ChatClient` injected
via constructor injection.

**Rationale**:
- Spring AI's `ChatClient` is a model-agnostic fluent API. Using it preserves the
  ability to swap OpenAI for another model (Anthropic, Mistral) by changing only
  the starter dependency and `spring.ai.*` configuration.
- The `PromptTemplate` class reads from a classpath `Resource` (e.g.
  `classpath:prompts/extraction.st`), keeping prompt content out of Java source
  files (Constitution Principle I — no magic strings).
- Auto-configuration reads `spring.ai.openai.api-key` from `${OPENAI_API_KEY}`.

**Prompt template invocation pattern**:
```java
PromptTemplate template = new PromptTemplate(promptResource);
Prompt prompt = template.create(Map.of("content", sourceContent));
String response = chatClient.call(prompt).getResult().getOutput().getContent();
```

**Dependency BOM entry** (add to `<dependencyManagement>` in pom.xml):
```xml
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-bom</artifactId>
  <version>1.0.0-M6</version>
  <type>pom</type>
  <scope>import</scope>
</dependency>
```

**application.yml additions**:
```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: ${OPENAI_MODEL:gpt-4o-mini}
          temperature: 0.2
```

**Alternatives Considered**:
- Direct OpenAI REST calls via `RestClient`: verbose, model-specific, not testable
  via mocks without HTTP interception.
- LangChain4j: heavier dependency; Spring AI is the Spring-native choice.

---

## R-003 — Redis Subscription Cache Serialization

**Decision**: `RedisTemplate<String, Object>` with `Jackson2JsonRedisSerializer<Object>`
using the application's `ObjectMapper`. Cache key: `"subscription:{userId}"`, TTL: 5 minutes
via `redisTemplate.expire(key, Duration.ofMinutes(5))`.

**Rationale**:
- `Jackson2JsonRedisSerializer` stores clean JSON without Java class metadata.
  This is safer than `JdkSerializationRedisSerializer` (breaks on class renames)
  and more explicit than `GenericJackson2JsonRedisSerializer` with `@class` meta.
- Using `Object` as the value type lets a single `RedisTemplate` bean serve
  multiple cache domains (subscription state, future feature flags, etc.).
- Keys are prefixed by domain (`subscription:`) to avoid collisions if other
  features add Redis keys later.

**Cache invalidation**:
After every processed Stripe subscription event, call:
```java
redisTemplate.delete("subscription:" + userId);
```
This must fire in `StripeWebhookHandler` after the DB write, not before.

**ENTERPRISE bypass**:
In `SubscriptionService.getSubscription(userId)`, check the DB tier before
consulting the cache. If tier is `ENTERPRISE`, skip cache lookup entirely and
call `stripeClient.fetchSubscription(userId)` directly.

**Alternatives Considered**:
- Spring Cache abstraction (`@Cacheable`): hides the invalidation call behind an
  annotation, making the ENTERPRISE bypass harder to express without a custom
  `CacheResolver`. Explicit `RedisTemplate` calls are clearer here.
- `GenericJackson2JsonRedisSerializer`: includes `@class` field in JSON — fragile
  on refactors, slightly larger payloads.

---

## R-004 — Hibernate 6 Soft Delete Pattern (`@SQLRestriction`)

**Decision**: Use `@SQLRestriction("deleted_at IS NULL")` on the `Project` entity
(not the deprecated `@Where` annotation).

**Rationale**:
- Spring Boot 3.2.x ships Hibernate ORM 6.4.x. In Hibernate 6.x, `@Where` was
  deprecated in favour of `@SQLRestriction` (introduced in Hibernate 6.3).
- `@SQLRestriction` applies transparently to all JPQL queries, `findById`, and
  collection loads — no query changes needed in `ProjectRepository`.
- `@SQLDelete(sql = "UPDATE projects SET deleted_at = NOW() WHERE id = ?")` on the
  entity causes Spring Data JPA's `deleteById(id)` to execute the soft-delete SQL
  instead of `DELETE FROM projects` — no custom repository method needed.

**Entity annotation pattern**:
```java
@Entity
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE projects SET deleted_at = NOW() WHERE id = ?")
@Table(name = "projects")
public class Project { ... }
```

**Alternatives Considered**:
- `@Where(clause="deleted_at IS NULL")`: deprecated in Hibernate 6.3, emits
  deprecation warnings at startup; should not be used in new code.
- Custom repository method: explicit but verbose; requires updating every query.
- Application-level filter in `ProjectRepository`: possible but leaks business
  logic into the data access layer.

---

## R-005 — @Async Exception Handling and Status Recording

**Decision**: Wrap the entire `@Async` method body in a `try-catch(Exception e)`
block. On catch: (1) call `webhookAuditService.recordFailed(eventId, e.getMessage())`
to persist `status = FAILED` with `error_message`, then (2) log `ERROR` with full
stack trace via SLF4J.

**Rationale**:
- Spring's `@Async` runs the method in a separate thread. Any uncaught exception
  **does not** propagate to the caller — it is silently discarded unless explicitly
  handled. The current `WebhookEventDispatcher` has no try-catch, meaning failures
  leave events in `ACCEPTED` status forever (zombie records).
- The `WebhookAuditService.recordFailed()` call must be inside the catch block,
  not in a `@AfterThrowing` AOP advice, to guarantee execution order.
- The `WebhookEvent` entity needs two new fields: `error_message TEXT` and an
  updated `status` enum including `PROCESSING` and `FAILED` states.

**Required `WebhookEvent.WebhookStatus` changes**:
- Add: `PROCESSING`, `FAILED` (alongside existing `ACCEPTED`, `REJECTED`)

**Pattern**:
```java
@Async("webhookExecutor")
public void dispatch(String eventId, ...) {
    try {
        webhookAuditService.setProcessing(eventId);
        // ... processing logic ...
    } catch (Exception ex) {
        log.error("Async dispatch failed for event {}: {}", eventId, ex.getMessage(), ex);
        webhookAuditService.recordFailed(eventId, ex.getMessage());
    }
}
```

**Alternatives Considered**:
- `AsyncUncaughtExceptionHandler`: global handler, cannot access the `eventId`
  contextually without ThreadLocal — overly complex for this use case.
- `CompletableFuture` return type: requires callers to check the future —
  incompatible with the fire-and-forget contract of the webhook handler.

---

## R-006 — OpenAI Retry with Exponential Backoff

**Decision**: Add `spring-retry` dependency and annotate the `OpenAIExtractionClient`
call site with `@Retryable(retryFor = {RuntimeException.class}, maxAttempts = 3,
backoff = @Backoff(delay = 1000, multiplier = 2.0))`. Add `@EnableRetry` to a config class.

**Rationale**:
- `spring-retry` is a first-class Spring project with zero friction in Spring Boot 3.
  The `@Retryable` annotation applies AOP-based retry with exponential backoff without
  any boilerplate try-catch-sleep loops.
- `maxAttempts = 3` + `delay = 1000ms` + `multiplier = 2.0` gives retries at 1s, 2s
  before failing on the third attempt — suitable for transient OpenAI rate-limit 429s.
- After exhausting retries, the exception propagates out of the `@Async` body where the
  catch block (R-005) records `status = FAILED`.

**pom.xml addition**:
```xml
<dependency>
  <groupId>org.springframework.retry</groupId>
  <artifactId>spring-retry</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework</groupId>
  <artifactId>spring-aspects</artifactId>
</dependency>
```
Note: `spring-aspects` is already present in the pom.xml.

**Alternatives Considered**:
- Manual retry loop: more verbose, harder to test, no backoff calculation utility.
- `Resilience4j`: powerful but a heavier dependency with a separate configuration model.
  Spring Retry is sufficient for 3-attempt linear exponential backoff.

---

## R-007 — Missing `WebhookEvent` Fields and Status States

**Decision**: Add `error_message TEXT` column, `dispatched_at` timestamp (already
present), and extend `WebhookStatus` to include `PROCESSING` and `FAILED` variants.
Also add `existsByPayloadHash(String hash)` to `WebhookEventRepository`.

**Rationale**:
- The current entity has only `ACCEPTED` and `REJECTED` in `WebhookStatus`. The spec
  requires a `FAILED` state (set by the async exception handler) and a `PROCESSING`
  state (set when dispatch begins, before completion).
- `existsByPayloadHash()` is more efficient than `findByPayloadHash().isPresent()` for
  the idempotency check — generates `SELECT 1 WHERE EXISTS(...)` instead of loading the
  full entity.
- These changes do NOT require a Flyway migration to the enum column because the status
  is stored as a VARCHAR string in PostgreSQL (via `@Enumerated(EnumType.STRING)`).
  Adding new values is non-breaking.

**Migration for `error_message`**: Part of V7+ migration group — add `error_message TEXT`
to `webhook_events` table.

---

## R-008 — `GeneratedDocumentation` Missing `PROCESSING` Status

**Decision**: Add `PROCESSING` to `GeneratedDocumentation.ProcessingStatus` enum.
The async extraction must set status to `PROCESSING` when dispatch starts, then
update to `COMPLETED` or `FAILED` on completion.

**Rationale**:
- Current enum: `PENDING`, `COMPLETED`, `FAILED`. The spec requires an async flow
  where the POST endpoint returns 202 with `status: PROCESSING` immediately.
- Without a `PROCESSING` state, clients polling `GET /extract-status/{docId}` cannot
  distinguish "not started" from "in progress".
- No DB migration needed — enum values stored as `VARCHAR` strings.

---

## R-009 — `StripeClient` Interface Extension for Phase 2

**Decision**: Extend the `StripeClient` interface with two new methods:
`createCheckoutSession(String userId, String priceId)` and
`createPortalSession(String customerId)`.

**Rationale**:
- `StripeClientImpl` is a new `@Bean` active on all non-`local` profiles.
- `LocalStripeClientStub` (active on `local` profile only) must also implement the
  two new methods to avoid breaking local development.
- Both methods return a simple `String` (the session URL) — no need for wrapper records.

**Required `StripeClient` additions**:
```java
String createCheckoutSession(String userId, String priceId);
String createPortalSession(String customerId);
```

---

## Summary of Resolved Unknowns

| ID | Unknown | Resolution |
|----|---------|------------|
| R-001 | Stripe SDK initialisation pattern | Static `Stripe.apiKey` in `@Bean`; `Webhook.constructEvent()` for verification |
| R-002 | Spring AI ChatClient version and API | `spring-ai-bom:1.0.0-M6`, `ChatClient.call(Prompt)` with classpath `.st` template |
| R-003 | Redis cache serialization strategy | `Jackson2JsonRedisSerializer<Object>` with `RedisTemplate<String, Object>` |
| R-004 | Hibernate 6 soft-delete annotation | `@SQLRestriction` + `@SQLDelete` — `@Where` is deprecated |
| R-005 | @Async exception handling | try-catch in `@Async` body → `recordFailed()` + `log.error()` |
| R-006 | Exponential backoff for OpenAI | `spring-retry` `@Retryable(maxAttempts=3, backoff=@Backoff(delay=1000, multiplier=2.0))` |
| R-007 | WebhookEvent missing states/fields | Add `PROCESSING`, `FAILED` to enum; add `error_message TEXT`; add `existsByPayloadHash()` |
| R-008 | GeneratedDocumentation missing PROCESSING | Add `PROCESSING` to `ProcessingStatus` enum |
| R-009 | StripeClient interface gaps | Add `createCheckoutSession()` and `createPortalSession()` to interface |
