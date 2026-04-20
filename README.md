# SyncDoc AI

> AI-Powered Documentation Synchronization Platform

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.x-blue.svg)](https://www.typescriptlang.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)

SyncDoc AI is an enterprise-grade real-time collaboration platform that combines instant messaging with AI-powered documentation synchronization. Built with a modern tech stack, it enables distributed teams to communicate seamlessly while automatically generating and maintaining documentation from code changes.

## ✨ Features

- **Real-Time Messaging** - Instant message delivery within 50ms across all connected clients
- **Persistent Message History** - Full conversation history with pagination and search
- **Presence Tracking** - Real-time status indicators (online/away/DND/offline)
- **Threaded Conversations** - Organized discussions with reply threads
- **GitHub Integration** - OAuth2-based repository connection with webhook processing
- **AI Documentation Generation** - Automatic change summaries using OpenAI/Spring AI
- **Stripe Billing** - Seamless subscription management with tiered pricing
- **WebSocket Updates** - Real-time notifications using STOMP over WebSocket

## 🛠️ Tech Stack

### Backend
| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 17 (LTS) | Core language with records, sealed classes |
| Spring Boot | 3.x | Application framework with auto-configuration |
| Spring Security | 6.x | JWT + OAuth2 resource server |
| Spring AI | 1.x | OpenAI integration for AI features |
| PostgreSQL | 16 | Primary database with JSONB support |
| Redis | 7.x | Caching and WebSocket pub/sub scaling |

### Frontend
| Technology | Version | Purpose |
|------------|---------|---------|
| React | 18 | UI library with concurrent features |
| TypeScript | 5.x | Type-safe JavaScript |
| Vite | 5.x | Build tool with sub-second HMR |
| Tailwind CSS | 3.x | Utility-first styling |
| Zustand | 4.x | Global state management |
| Axios | 1.x | HTTP client with interceptors |

## 🚀 Quick Start

### Prerequisites

- Java 17+
- Node.js 18+
- Docker & Docker Compose
- Maven 3.8+

### 1. Start Infrastructure

```bash
docker compose up -d
docker compose ps
```

This starts PostgreSQL, Redis, and Elasticsearch containers.

### 2. Start Backend

```bash
cd backend
mvn spring-boot:run
```

Backend will be available at `http://localhost:8080`

### 3. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend will be available at `http://localhost:5173`

### Verify Setup

```bash
# Check backend health
curl -sS http://localhost:8080/api/v1/workspaces

# Check frontend
curl -I http://localhost:5173
```

## 📁 Project Structure

```
syncdoc-ai/
├── backend/                 # Spring Boot application
│   ├── src/main/java/      # Java source code
│   └── src/test/           # Unit & integration tests
├── frontend/               # React SPA
│   ├── src/
│   │   ├── api/           # API client modules
│   │   ├── components/    # Reusable UI components
│   │   ├── features/      # Feature modules
│   │   └── store/         # Zustand stores
│   └── tests/             # Frontend tests
├── docs/                   # Documentation
├── specs/                  # Feature specifications
├── scripts/                # Utility scripts
└── tests/                  # E2E & integration tests
```

## 🧪 Testing

```bash
# Backend tests
cd backend && mvn test

# Frontend unit tests
cd frontend && npm test

# E2E tests
cd frontend && npm run test:e2e
```

## 📋 Development Sprints

| Sprint | Focus | Status |
|--------|-------|--------|
| Sprint 1 | Core API & Auth (Walking Skeleton) | ✅ Complete |
| Sprint 2 | GitHub Integration & Data Flow | ✅ Complete |
| Sprint 3 | Stripe Monetization | 🔄 In Progress |
| Sprint 4 | AI & RAG Foundation | 📋 Planned |
| Sprint 5 | System Design & DevOps | 📋 Planned |

---

## 🗂️ My GitHub Repositories

| Repository | Description | Language | Visibility |
|------------|-------------|----------|------------|
| [SyncDoc-AI](https://github.com/ankit-dev42/SyncDoc-AI) | AI-Powered Documentation Synchronization Platform | Java | Private |
| [aesthetics-products](https://github.com/ankit-dev42/aesthetics-products) | Website for Insta and YouTube channel products links | TypeScript | Private |
| [parameter-golf](https://github.com/ankit-dev42/parameter-golf) | Train the smallest LM that fits in 16MB | Python | Public |
| [system-design-concepts](https://github.com/ankit-dev42/system-design-concepts) | System design learning resources | - | Public |
| [Agent_Upgrad](https://github.com/ankit-dev42/Agent_Upgrad) | AI Agent development project | Jupyter Notebook | Public |
| [claw-code](https://github.com/ankit-dev42/claw-code) | Fastest repo to surpass 100K stars - Built in Rust | Rust | Public |
| [outskill](https://github.com/ankit-dev42/outskill) | Outskill demos and examples | Python | Public |
| [atlas](https://github.com/ankit-dev42/atlas) | Advanced RAG System for YouTube video analysis & assignments | Python | Public |
| [coding-practice-platform](https://github.com/ankit-dev42/coding-practice-platform) | Coding practice and learning platform | Shell | Private |
| [spring-app-config](https://github.com/ankit-dev42/spring-app-config) | Spring application configuration templates | - | Public |

---

## 📄 License

This project is proprietary software. All rights reserved.

## 👤 Author

**Ankit Kumar** - [@ankit-dev42](https://github.com/ankit-dev42)
