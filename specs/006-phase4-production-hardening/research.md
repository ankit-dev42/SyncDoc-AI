# Research: Phase 4 — Production Hardening

**Generated**: 2026-05-07  
**Branch**: `006-phase4-production-hardening`  
**Status**: All NEEDS CLARIFICATION resolved

---

## 1. Testcontainers for PostgreSQL Integration Tests

**Decision**: Add `testcontainers-bom` to `dependencyManagement`, add `testcontainers:postgresql` and `testcontainers:junit-jupiter` artifacts (test scope). Use a single shared `static PostgreSQLContainer<?>` per class to minimize container startup overhead.

**Pattern**:
```java
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
abstract class AbstractIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16").withReuse(true);

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
```

**Surefire / Failsafe split**: `*IT.java` tests run under `maven-failsafe-plugin` (`mvn verify`). Plain `*Test.java` run under Surefire (`mvn test`). Failsafe needs `<include>**/*IT.java</include>` and explicit `integration-test` + `verify` lifecycle binding.

**Migration count**: At plan time, V1–V12 migrations exist. The `AbstractIntegrationTest` runs all of them on a fresh container. The spec reference to V1–V11 is superseded by the actual count.

**Alternatives considered**: `@DataJpaTest` with H2 — rejected because H2 does not support JSONB, `pg_advisory_lock`, or PostgreSQL-specific index expressions that appear in V7 and V8.

---

## 2. Structured Audit Logging with logstash-logback-encoder

**Decision**: Use `net.logstash.logback:logstash-logback-encoder:7.4` (latest stable, compatible with Spring Boot 3.2.x / Logback 1.4.x). Deploy a profile-switched `logback-spring.xml` that outputs JSON in non-`local` profiles and pattern text in `local`.

**MDC field strategy**: Before emitting each audit log line, populate MDC with envelope fields (`event`, `userId`, `ip`, `timestamp`, `traceId`). The `LogstashEncoder` auto-promotes all MDC entries to top-level JSON properties — no custom encoder needed. Clear MDC in a `finally` block.

```xml
<!-- logback-spring.xml excerpt (non-local profile) -->
<springProfile name="!local">
  <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
  </appender>
  <logger name="AUDIT" level="INFO" additivity="false">
    <appender-ref ref="JSON"/>
  </logger>
</springProfile>
```

**Sensitive field exclusion**: `AuditLogger` must never call `MDC.put()` with keys `accessToken`, `password`, `stripeSecret`, or `email`. Use a `SanitizingAuditLogger` wrapper pattern (static validation in tests via `AuditLoggerStructuredOutputTest`).

**Alternatives considered**: Splunk HEC direct appender — rejected (adds network dependency to logging path). Custom Jackson serialiser — rejected (logstash-logback-encoder covers this without custom code).

---

## 3. Micrometer MeterRegistry Integration

**Decision**: Spring Boot Actuator (already in pom from Phase 2) auto-configures a `CompositeMeterRegistry`. Inject `MeterRegistry` into `SubscriptionService` constructor. Register a `Counter` (`subscription.check.total`) and a `Timer` (`subscription.check.duration`) at bean construction time. Keep existing `MetricsService` for backwards-compat health endpoint; the new Micrometer metrics are in addition, not a replacement.

**Metric names**:
- `subscription.check.total` — counter, tags: `tier`, `result` (`ALLOW`/`DENY`/`ENTERPRISE_BYPASS`)
- `subscription.check.duration` — timer, tags: `tier`

**Actuator exposure** (application.yml addition):
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,env,loggers
  endpoint:
    health:
      show-details: when-authorized
      show-components: when-authorized
```

**Alternatives considered**: Micrometer Prometheus registry — deferred (no Prometheus in current stack). Custom `/api/v1/health` kept alongside Actuator `/actuator/health` — rejected per spec (spec requires Actuator health, not custom endpoint).

---

## 4. Actuator Endpoint Security

**Decision**: In `WebSecurityConfig.filterChain()`, add a `hasRole("ADMIN")` rule for `/actuator/**` AFTER the existing `permitAll()` for `/actuator/health` and `/actuator/info`. Spring Security evaluates rules in order — first match wins.

**Current state**: Only `/actuator/health` and `/actuator/info` are in the `permitAll()` block. All other routes fall through to `.anyRequest().authenticated()` — which means `/actuator/env` requires a *valid JWT* but not the ADMIN role. Phase 4 tightens this to `hasRole("ADMIN")`.

**Rule addition**:
```java
.requestMatchers("/actuator/**").hasRole("ADMIN")
.anyRequest().authenticated()
```

This goes between the `permitAll()` block and `anyRequest()`.

**Alternatives considered**: Separate `SecurityFilterChain` for management endpoints — rejected (adds complexity; single chain is sufficient since we have custom JWT auth).

---

## 5. ENTERPRISE Tier — SyncAuthorizationService

**Decision**: The existing method is `evaluate(userId, currentSyncCount)`. The spec uses `authorize()` — this is a new method name. Add `authorize(String userId, int currentSyncCount)` that:
1. Fetches subscription via `subscriptionService.getSubscription(userId)`
2. If `ENTERPRISE` → call `subscriptionService.forceRefreshFromStripe(userId)` + return `AuthorizationResult.ALLOW`
3. Otherwise delegate to existing `evaluate()` logic

**`forceRefreshFromStripe()` in SubscriptionService**: Phase 2 T128 was supposed to implement this but it is absent. Phase 4 adds it:
- Delete the Redis cache key for the user
- Call `stripeClient.fetchSubscription(userId)` directly
- Persist the result
- Return the refreshed `UserSubscription`

**ENTERPRISE semantics** (per clarification Q2/Q3):
- Zero repo count check — unconditional ALLOW
- Redis cache completely bypassed on every check (not just on demand)
- PRO and FREE paths unchanged

**Alternatives considered**: Feature flag toggling — rejected (ENTERPRISE is a defined tier; conditional on subscription object is the correct model).

---

## 6. CollaborationHealthIndicator Conversion

**Decision**: The existing `CollaborationHealthIndicator` is a `@RestController` at `/api/v1/health`. Phase 4 converts it to implement Spring Boot's `HealthIndicator` interface so it integrates with `/actuator/health`. The custom `/api/v1/health` endpoint can be retained for backwards compatibility but is no longer the primary health signal.

**`HealthIndicator` pattern**:
```java
@Component("collaboration")
public class CollaborationHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        // check Redis ping + verify DB connectivity
        return Health.up()
            .withDetail("redis", "OK")
            .withDetail("db", "OK")
            .build();
    }
}
```

**Redis `HealthContributor`**: Spring Boot Actuator auto-configures a `RedisHealthIndicator` if `spring-boot-starter-data-redis` is present. Since it's already in the pom, a `RedisHealthContributor` bean just needs to be confirmed present (no code needed; Spring auto-detects the `RedisConnectionFactory`). The spec requires this to be explicit — add `@Bean RedisHealthContributor` manually to make intent clear.

---

## 7. CONSTITUTION_COMPLIANCE.md

**Decision**: Live at `docs/CONSTITUTION_COMPLIANCE.md`. Format: markdown table with columns `Rule`, `Description`, `Status`, `Evidence`. Committed by engineering lead as part of the Phase 4 merge PR. Engineering lead sign-off = PR approval (per clarification Q5).

**Template**:
| Rule | Description | Status | Evidence |
|------|-------------|--------|---------|
| 01 | No hardcoded secrets | PASS | `git grep -E "sk_live\|sk_test"` → 0 results |
| 02 | All webhook payloads verified | PASS | `StripeWebhookSignatureVerifier`, `GitHubWebhookSignatureVerifier` |
| ... | ... | ... | ... |

---

## Resolved Clarifications

| Q | Answer |
|---|--------|
| Q1: Actuator endpoint security | `/health`, `/info` public; `/env`, `/metrics`, `/loggers`, all others → `hasRole("ADMIN")` |
| Q2: Audit log fields | Envelope: `event`, `userId`, `ip`, `timestamp`, `traceId`. Never: `accessToken`, `password`, raw secrets |
| Q3: ENTERPRISE unlock | Cache bypass on every request + unlimited repo syncs; `forceRefreshFromStripe()` called always |
| Q4: Testcontainers strategy | Alongside unit tests; Surefire runs `*Test.java`, Failsafe runs `*IT.java` |
| Q5: Compliance doc location | `docs/CONSTITUTION_COMPLIANCE.md`; sign-off via engineering lead PR approval |
