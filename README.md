
---
# CodeDuel 

> **Transforming passive coding practice into an addictive, real-time multiplayer arena.**

**CodeDuel AI** is a real-time multiplayer platform that turns algorithmic practice into an engaging head-to-head competitive experience. Instead of solving problems in isolation, developers duel 1v1 in real-time, accompanied by a **Socratic AI Coach** that provides contextual hints, post-match reviews, and insights under pressure.

---

## Key Features

*   **Real-time Arena:** 1v1 live coding duels synced via WebSockets with server-authoritative timers and live status tracking.
*   **Smart Matchmaking:** ELO-based matchmaking using Redis Sorted Sets to pair developers of similar skill levels.
*   **Socratic AI Coach:** Dynamic, non-intrusive hint system (ranging from vague conceptual nudges to specific guidance) and post-match performance analyses.
*   **Secure Code Sandboxing:** Remote, isolated execution of user code via Judge0 API integration.
*   **Global Leaderboards:** Real-time top-rank tracking with high-performance caching.

---

## Tech Stack & Architecture

### Backend & Distributed Systems
*   **Spring Boot (Java 21):** Core backend modular monolith handling Auth, Arena, and Business logic.
*   **Spring Security & JWT:** Stateless authentication with access tokens and HTTP-only refresh cookies.
*   **Apache Kafka:** Event bus driving asynchronous cross-module communication (e.g., `MatchCreated`, `SubmissionEvaluated`, `AIAnalysisRequested`).
*   **Redis:** Fast in-memory storage driving the matchmaking queues, live match states, and temporary cache.
*   **PostgreSQL:** Relational database for persistent storage (Users, Submissions, Match Histories, AI Reviews).
*   **Flyway / Liquibase:** Schema migrations version-controlled in the codebase.

### Frontend
*   **React (Vite):** Lightweight, responsive client-side UI.
*   **STOMP over WebSockets:** Live bidirectional updates for typing indicators, timers, and test execution results.
*   **Monaco Editor:** Integrated IDE experience in the browser.

### AI Integration
*   **Spring AI:** Connected to LLMs (GPT-4o-mini / Gemini Flash) for generating Socratic hints and performance reviews.

### DevOps & Infrastructure
*   **Docker & Docker Compose:** Containerized orchestration of Postgres, Redis, and Kafka for local development.
*   **CI/CD (Jenkins):** Multi-stage Docker builds to compile Spring Boot and deploy to target servers.

---

## System Flow

1.  **Queue:** User joins the matchmaking queue -> Redis matches them based on ELO.
2.  **Match Initialization:** Match created in DB -> Event broadcasted via Kafka -> Clients notified.
3.  **Active Arena:** Real-time bi-directional STOMP messages manage game timers, typing status, and submission triggers.
4.  **Execution & Evaluation:** Submitted code is securely executed -> Result evaluated and broadcasted.
5.  **Post-Match Analysis:** AI triggers asynchronously to critique code efficiency and store reviews.

---