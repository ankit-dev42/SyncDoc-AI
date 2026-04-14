# SyncDoc AI: Product Roadmap & Sprint Tasks

## Sprint 1: The "Walking Skeleton" (Core API & Auth)
**Goal:** Establish a secure, end-to-end communication channel between Frontend and Backend.

* **Task 1.1: Backend Base Setup**
    * Configure Spring Boot with Java 17, Spring Security (JWT), and PostgreSQL.
    * Implement Folder Structure: Module-based (User, Project, Auth).
* **Task 1.2: Frontend Foundation**
    * Setup React + TypeScript + Vite + Tailwind CSS.
    * Implement Axios Interceptors for JWT handling.
* **Task 1.3: User Identity Service**
    * Develop Registration/Login APIs.
    * Frontend Auth Pages (Login, Sign-up) with form validation.
* **Task 1.4: Database Schema Design**
    * Design Entities: `User`, `Project`, `ApiToken`, `SyncLogs`.

## Sprint 2: The "Plumbing" (GitHub Integration & Data Flow)
**Goal:** Connect to the external world and handle real data.

* **Task 2.1: External API Integration**
    * Implement OAuth2 Flow for GitHub App integration.
    * Create a "Project Dashboard" where users can see their repositories.
* **Task 2.2: The Webhook Receiver**
    * Build a REST endpoint to receive GitHub `push` and `pull_request` payloads.
    * Implement a "Signature Verifier" for security (validating GitHub secrets).
* **Task 2.3: Async Processing with Spring Events**
    * Move Webhook processing to an `@Async` thread to avoid blocking the caller.
    * Log every event in the database for the "History" UI.

## Sprint 3: The "Monetization" (Stripe Integration)
**Goal:** Transform the project into a professional SaaS product.

* **Task 3.1: Stripe Product Setup**
    * Configure Stripe Dashboard (Free Tier vs. Pro Tier).
    * Backend: Implement `/create-checkout-session` endpoint.
* **Task 3.2: Subscription Management**
    * Frontend: Add a "Pricing Table" and "Billing Portal" button.
    * Backend: Handle Stripe Webhooks (`invoice.paid`, `customer.subscription.deleted`).
* **Task 3.3: Feature Gating**
    * Implement logic to restrict the number of synced repos based on the Stripe Plan.

## Sprint 4: The "Intelligence" (AI & RAG Foundation)
**Goal:** Add value using Large Language Models.

* **Task 4.1: Spring AI Integration**
    * Integrate `spring-ai-openai-spring-boot-starter`.
    * Develop a "Prompt Template" for code summarization.
* **Task 4.2: Documentation Generation Engine**
    * Extract file diffs from GitHub commits.
    * Send diffs to AI to generate a "Change Summary" in Markdown.
* **Task 4.3: Real-time UI Updates**
    * Implement **WebSockets** (using STOMP) to notify the user in React when a doc update is ready.

## Sprint 5: The "Polishing" (System Design & Devops)
**Goal:** Prove the project is production-ready.

* **Task 5.1: Containerization**
    * Write `Dockerfile` for Backend/Frontend and `docker-compose.yml` for local env.
* **Task 5.2: Caching & Optimization**
    * Implement **Redis** caching for frequently accessed Project metadata.
* **Task 5.3: Unit & Integration Testing**
    * Achieve 70%+ coverage using JUnit 5 (Backend) and React Testing Library (Frontend).