# syncdoc-ai Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-04-20

## Active Technologies

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
- 004-phase2-business-logic: Added stripe-java 23.x, spring-ai-openai-spring-boot-starter (spring-ai-bom 1.0.0-M6), spring-retry, Redis subscription cache (Jackson2JsonRedisSerializer), Hibernate 6 @SQLRestriction soft-delete, @Async exception handling pattern, Flyway V7–V11 migrations

- 003-phase1-foundation-security: Added Java 17 (LTS), Spring Boot 3.2.x + Spring Security 6, jjwt 0.12.x (jjwt-api / jjwt-impl / jjwt-jackson), Spring Data JPA, Flyway 10, BCrypt (via Spring Security)

<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
