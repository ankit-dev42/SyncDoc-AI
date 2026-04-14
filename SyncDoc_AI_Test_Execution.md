# SyncDoc AI: Quality Assurance & Business Goal Validation

This document contains prompts and instructions for GitHub Copilot to execute and verify that the application meets all business requirements.

## 1. Automated Test Generation (Java 17 / Spring Boot 3)

### Core Business Logic Validation
> **Copilot Prompt:** "@workspace Generate JUnit 5 test cases for `SubscriptionService`. Ensure it correctly validates if a user has an active Stripe subscription before allowing a 'Sync' operation. Mock the `StripeClient` and verify that a 'Free' user cannot sync more than 1 repository."

### Security & Access Control
> **Copilot Prompt:** "@workspace Write integration tests for the `ProjectController`. Use `MockMvc` to verify that a user can only access their own project documentation and receives a 403 Forbidden when trying to access another user's `projectId`."

### Webhook Reliability
> **Copilot Prompt:** "@workspace Create a test utility to simulate a GitHub Webhook POST request with a valid HMAC signature. Verify that the `WebhookController` successfully pushes a message to the internal event bus and returns a 202 Accepted status."

## 2. Business Flow Verification (E2E)

### Payment Success Path
> **Copilot Prompt:** "Generate a Playwright/Cypress test script that: 
1. Logs into the dashboard.
2. Clicks on the 'Upgrade to Pro' button.
3. Redirects to the Stripe checkout page.
4. Completes the payment using a test card.
5. Verifies the user is redirected back with a 'Success' message."

### AI Documentation Quality
> **Copilot Prompt:** "Create a unit test for `AIProcessingService`. Mock the OpenAI API response with a sample Markdown string. Assert that the service correctly extracts 'Key Changes' and 'Action Items' from the generated text and saves it to the database."

---

## 3. Deployment Readiness Checklist
- [ ] **Java 17 Compliance:** Ensure no Java 21 features (like Sequenced Collections or unnamed classes) are used in production code.
- [ ] **Environment Secrets:** Verify that `STRIPE_API_KEY`, `GITHUB_CLIENT_ID`, and `OPENAI_API_KEY` are not hardcoded.
- [ ] **Database Integrity:** Run `liquibase` or `flyway` migrations and verify the `user_subscriptions` table schema.
