# 🌿 Mindful Finance

**The Calm Architect's approach to financial freedom.**

## 🎯 The Vision
Most financial apps are designed to keep you addicted with flashing numbers and constant alerts. **Mindful Finance** is built for the "End Game." It prioritizes data integrity (The Truth) and psychological peace (The Calm).

## 🛠 The Tech Stack
- **Backend:** Java 21, Spring Boot 3.x, PostgreSQL.
- **Frontend:** React, Vite, TypeScript, Tailwind CSS.
- **Architecture:** Hexagonal / Clean Architecture.

## ✅ Running tests
Prereqs: Java 21 + Maven.

From repo root:
`mvn -f backend/pom.xml test`

## 🐘 Local PostgreSQL Workflow
Prereqs: Java 21, Maven, Docker.

Start Postgres:
```bash
docker compose -f backend/docker-compose.yml up -d
```

The local database is exposed on `localhost:55432` to avoid conflicts with an already-installed PostgreSQL on `5432`.

If you run the `api` module by itself, install backend modules once from repo root so Maven can resolve sibling artifacts:
```bash
mvn -f backend/pom.xml install
```

Run the API with the Postgres profile:
```bash
mvn -f backend/api/pom.xml -Dspring-boot.run.profiles=postgres spring-boot:run
```

Stop local Postgres:
```bash
docker compose -f backend/docker-compose.yml down
```

## 🧘 Principles of the System
1. **Truth First:** No floating-point math for money. `BigDecimal` or nothing.
2. **Context over Noise:** Show long-term trends, not daily market jitters.
3. **Calm UI:** No intrusive notifications. Information is served only when requested.
4. **Logic-Driven:** Inspired by Chess strategy—evaluate the position, don't just react to the last move.

## 🚀 Roadmap
- [ ] Phase 1: Core Truth Engine (Backend Domain Logic)
- [ ] Phase 2: Ingestion & Peace Calculator
- [ ] Phase 3: The Calm Interface (React MVP)
