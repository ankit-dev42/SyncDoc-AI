# API Contracts: Phase 4 — Production Hardening

**Generated**: 2026-05-07  
**Branch**: `006-phase4-production-hardening`  
**Scope**: Actuator endpoint security contracts + audit event log schema contract

---

## Actuator Endpoint Access Contract

Phase 4 adds ADMIN role enforcement on all non-public Actuator endpoints. This is a change from the current state where any valid JWT user can access `/actuator/env` and `/actuator/metrics`.

### Public Endpoints (no auth required)

| Endpoint | Method | Response |
|----------|--------|----------|
| `/actuator/health` | GET | `{ "status": "UP\|DOWN", "components": { ... } }` (summary only without ADMIN) |
| `/actuator/info` | GET | `{ "build": { ... } }` |

### Admin-Only Endpoints (`hasRole("ADMIN")` required)

| Endpoint | Method | Without ADMIN | With ADMIN |
|----------|--------|---------------|------------|
| `/actuator/env` | GET | 401 | Environment properties |
| `/actuator/metrics` | GET | 401 | Metrics list |
| `/actuator/metrics/{name}` | GET | 401 | Specific metric value |
| `/actuator/loggers` | GET | 401 | Logger levels |
| `/actuator/loggers/{name}` | POST | 401 | Update logger level |

### Health Response Contract

**Without ADMIN token** (summary only):
```json
{
  "status": "UP"
}
```

**With ADMIN token** (full detail):
```json
{
  "status": "UP",
  "components": {
    "collaboration": {
      "status": "UP",
      "details": {
        "redis": "OK",
        "db": "OK"
      }
    },
    "db": {
      "status": "UP"
    },
    "redis": {
      "status": "UP"
    }
  }
}
```

**When DB is unreachable**:
```json
{
  "status": "DOWN",
  "components": {
    "db": { "status": "DOWN" }
  }
}
```

---

## Audit Log Event Contract

Every audit event emitted by `AuditLogger` must conform to this JSON schema when `logstash-logback-encoder` is active (all non-local profiles).

### Envelope Fields (always present)

```json
{
  "@timestamp": "2026-05-07T12:00:00.000Z",
  "level": "INFO",
  "logger_name": "AUDIT",
  "message": "audit event",
  "event": "AUTH_LOGIN",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "ip": "192.168.1.1",
  "traceId": "abc123def456"
}
```

### Prohibited Fields

The following keys MUST NEVER appear in any audit log entry:

| Prohibited Key | Reason |
|----------------|--------|
| `accessToken` | JWT token exposure |
| `password` | Credential exposure |
| `passwordHash` | Credential exposure |
| `stripeApiKey` | Payment secret |
| `webhookSecret` | Signing secret |
| `jwtSecret` | Signing secret |
| `email` | PII (only UUID `userId` allowed) |

### Event-Specific Contract

#### AUTH_LOGIN
```json
{
  "event": "AUTH_LOGIN",
  "userId": "<uuid>",
  "ip": "<ip>",
  "traceId": "<id>"
}
```

#### BILLING_WEBHOOK_RECEIVED
```json
{
  "event": "BILLING_WEBHOOK_RECEIVED",
  "userId": "-",
  "ip": "<ip>",
  "stripeEventId": "evt_xxx",
  "type": "checkout.session.completed",
  "traceId": "<id>"
}
```

#### AI_EXTRACTION_COMPLETED
```json
{
  "event": "AI_EXTRACTION_COMPLETED",
  "userId": "<uuid>",
  "docId": "<uuid>",
  "qualityScore": 0.87,
  "durationMs": 1420,
  "traceId": "<id>"
}
```

---

## Metrics Contract

### `subscription.check.total` (Counter)

| Tag | Values |
|-----|--------|
| `tier` | `FREE`, `PRO`, `ENTERPRISE` |
| `result` | `ALLOW`, `DENY`, `ENTERPRISE_BYPASS` |

### `subscription.check.duration` (Timer)

| Tag | Values |
|-----|--------|
| `tier` | `FREE`, `PRO`, `ENTERPRISE` |

Accessible at `/actuator/metrics/subscription.check.total` (ADMIN only).
