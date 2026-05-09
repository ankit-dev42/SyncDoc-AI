# syncdoc-ai Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-05-08

## Active Technologies
- TypeScript 5.2 (frontend) · Java 17 (backend) (005-frontend-devops)
- sessionStorage (accessToken only) — no new database changes in this phase (005-frontend-devops)
- Java 17 (LTS) + Spring Boot 3.2.x, Spring Security 6, Spring Data JPA, Micrometer (via spring-boot-starter-actuator), logstash-logback-encoder 7.4, Testcontainers (testcontainers-bom + postgresql + junit-jupiter), maven-failsafe-plugin (006-phase4-production-hardening)
- PostgreSQL 16 (production + Testcontainers IT), Redis 7 (existing, no changes) (006-phase4-production-hardening)

- Java 17 (LTS), Spring Boot 3.2.x + Spring Security 6, jjwt 0.12.x (jjwt-api / jjwt-impl / jjwt-jackson), Spring Data JPA, Flyway 10, BCrypt (via Spring Security) (003-phase1-foundation-security)
- stripe-java 23.x, Spring AI 1.0.0-M6 (OpenAI ChatClient), spring-retry, Redis (Jackson2JsonRedisSerializer), Hibernate 6 @SQLRestriction, bounded @Async TaskExecutor (004-phase2-business-logic)

## Project Structure

```text
backend/
frontend/
tests/
```

## Commands

# Add commands for Java 17 (LTS), Spring Boot 3.2.x

## Code Style

Java 17 (LTS), Spring Boot 3.2.x: Follow standard conventions

## Recent Changes
- 006-phase4-production-hardening: Added Java 17 (LTS) + Spring Boot 3.2.x, Spring Security 6, Spring Data JPA, Micrometer (via spring-boot-starter-actuator), logstash-logback-encoder 7.4, Testcontainers (testcontainers-bom + postgresql + junit-jupiter), maven-failsafe-plugin
- 006-phase4-production-hardening: Added [if applicable, e.g., PostgreSQL, CoreData, files or N/A]
- 005-frontend-devops: Added TypeScript 5.2 (frontend) · Java 17 (backend)


<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
