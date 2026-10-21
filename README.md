<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="docs/assets/shipshape-logo-dark.png" />
    <img src="docs/assets/shipshape-logo.png" alt="ShipShape logo" width="420" />
  </picture>
</p>

<h3 align="center">Monitor. Deploy. Stay ShipShape.</h3>

<p align="center">
  A developer platform that monitors HTTP services, detects incidents, and answers one question:<br/>
  <strong>“Did this deploy cause this outage?”</strong>
</p>

<p align="center">
  <img alt="Java 21" src="https://img.shields.io/badge/Java-21-0B2D5B" />
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-modular%20monolith-2563EB" />
  <img alt="React + TypeScript" src="https://img.shields.io/badge/React-TypeScript-38BDF8" />
  <img alt="PostgreSQL" src="https://img.shields.io/badge/PostgreSQL-Flyway-14B8A6" />
  <img alt="Status" src="https://img.shields.io/badge/status-in%20development-E5E7EB" />
</p>

---

> **Status:** Active development.
> Features below are marked ✅ implemented, 🚧 in progress, or ⏳ planned. Nothing is labelled done until it is implemented **and** covered by tests.

## Table of contents

- [Why I built this](#why-i-built-this)
- [Why ShipShape](#why-shipshape)
- [Features](#features)
- [Architecture](#architecture)
- [Tech stack](#tech-stack)
- [Getting started](#getting-started)
- [Running tests](#running-tests)
- [Project structure](#project-structure)
- [Security](#security)
- [Deployment](#deployment)
- [Roadmap](#roadmap)
- [Known limitations](#known-limitations)
- [Author](#author)

## Why I built this

After leading development on AU Connect, a university platform deployed at life.au.edu, I learned that shipping is only half the job. Once real users depend on it, the questions change: *Is it up? Is it slow? And if something broke, was it the release we just pushed?* ShipShape is the tool I wanted for that: one place that watches your services and connects outages to the deploys that preceded them.

## Why ShipShape

Most outages start with a change. Uptime monitors tell you *that* something broke, and CI/CD tools tell you *what* shipped, but they rarely talk to each other. ShipShape puts both in one place:

1. **Monitor** your HTTP endpoints on a schedule.
2. **Detect** incidents when checks fail repeatedly, and resolve them when the service recovers.
3. **Correlate** each incident with deployments that happened shortly before it, and explain *why* each deploy is a suspect.

## Features

| Feature | Status |
| --- | --- |
| Foundations: Spring Boot skeleton, Flyway migrations, Testcontainers integration test, Modulith boundary verification | 🚧 |
| Account registration and login (session cookies, CSRF protection) | ⏳ |
| Projects and monitors with per-user ownership checks | ⏳ |
| Scheduled HTTP checks with connect/read timeouts | ⏳ |
| SSRF protection (blocks private, loopback, link-local and cloud-metadata addresses after DNS resolution) | ⏳ |
| Incident detection (open after N consecutive failures, resolve after M successes) | ⏳ |
| Deployment ingest API with per-project API tokens (hashed, shown once) | ⏳ |
| Incident ↔ deployment correlation with match explanations | ⏳ |
| Dashboard: latency and uptime charts, incident timeline with deploy markers | ⏳ |
| CI/CD: GitHub Actions → GHCR images | ⏳ |
| Infrastructure as code: Terraform on AWS EC2 | ⏳ |
| Dogfooding: ShipShape reports its own deploys to itself | ⏳ |

## Architecture

ShipShape is a **modular monolith**: one deployable Spring Boot application split into business modules with boundaries enforced by [Spring Modulith](https://spring.io/projects/spring-modulith) tests.

```
                ┌───────────────────────────────────────────────┐
  Browser ────▶ │  React + TypeScript SPA (Vite, TanStack Query) │
                └───────────────────────┬───────────────────────┘
                                        │ JSON over HTTPS (session cookie + CSRF)
                ┌───────────────────────▼───────────────────────┐
                │           Spring Boot modular monolith         │
                │                                                │
                │  identity ─ monitoring ─ incidents ─ deployments│
                │      │           │            │           │     │
                └──────┼───────────┼────────────┼───────────┼─────┘
                       └───────────┴─────┬──────┴───────────┘
                                         ▼
                               PostgreSQL (Flyway)
                                         ▲
  CI pipeline ── POST /api/deployments ──┘   (API token)
```

| Module | Responsibility |
| --- | --- |
| `identity` | Users, authentication, sessions |
| `monitoring` | Monitors, scheduler, HTTP checker, check results |
| `incidents` | Incident detection and lifecycle |
| `deployments` | Deploy ingest, API tokens, correlation |
| `shared` | Small common types used by all modules |

**Why not microservices?** Each module talks to the others through a narrow public API, so if monitoring ever needed to scale on its own, the module boundary is already the seam to split along. Until then, a single process avoids network calls, distributed transactions and extra infrastructure cost.

## Tech stack

- **Backend:** Java 21, Spring Boot 4, Spring Security (session cookies), Spring Data JPA, Spring Modulith, PostgreSQL 17, Flyway
- **Frontend:** React, TypeScript, Vite, Tailwind CSS, TanStack Query, Recharts
- **Testing:** JUnit 5, Testcontainers, WireMock, Vitest
- **DevOps:** Docker Compose, GitHub Actions, GitHub Container Registry, Terraform, AWS EC2

## Getting started

### Prerequisites

- Java 21
- A Docker runtime (Docker Desktop, OrbStack or Colima)
- Node.js (LTS) and npm, for the frontend once it is added

### 1. Configure and start the database

```bash
cp .env.example .env
# edit .env and set POSTGRES_PASSWORD (for example: openssl rand -hex 16)
docker compose up -d
docker compose ps   # wait until the status shows (healthy)
```

PostgreSQL is published on `127.0.0.1:5433` by default. Set `POSTGRES_PORT` in `.env` to change it.

> Local credentials live in `.env`, which is gitignored and never committed. `.env.example` lists the required variables with placeholder values.

### 2. Run the backend

```bash
cd backend
./mvnw spring-boot:run
```

The backend reads the same `.env` file, and Flyway applies database migrations on startup. Check it is up:

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

### 3. Run the frontend

The frontend is coming in a later step of the roadmap.

## Running tests

```bash
# Backend: integration tests (Testcontainers starts its own PostgreSQL) + module boundary checks
cd backend && ./mvnw test
```

Backend tests do **not** use the Compose database: Testcontainers starts a throwaway PostgreSQL 17 container and Flyway builds the schema from scratch on every run. Docker must be running.

## Project structure

```
shipshape/
├── backend/            Spring Boot application
│   └── src/main/java/com/shipshape/
│       ├── identity/
│       ├── monitoring/
│       ├── incidents/
│       ├── deployments/
│       └── shared/
├── frontend/           React + TypeScript SPA (planned)
├── infra/              Terraform for AWS (planned)
├── docs/               Architecture notes and assets
├── .env.example
└── docker-compose.yml
```

## Security

Planned security design. Each item is marked ✅ once it is implemented and tested.

- ⏳ **Ownership checks:** every query for a project, monitor, incident or deployment is scoped to the current user; requests for another user's resources return `404`, not `403`, to avoid leaking existence.
- ⏳ **SSRF protection:** monitors accept user-supplied URLs, so the checker resolves DNS first and rejects private, loopback, link-local and metadata (`169.254.169.254`) addresses, including after redirects.
- ⏳ **API tokens:** deploy-ingest tokens are random, stored only as hashes, and shown to the user once.
- ⏳ **Sessions:** HTTP-only, `SameSite` session cookies with CSRF protection for state-changing requests.
- ✅ **Local secrets:** database credentials come from a gitignored `.env`; the local database port is bound to `127.0.0.1` only.
- ⏳ **Production secrets:** will come from AWS SSM Parameter Store, never from the repository.

## Deployment

Planned. ShipShape is built and tested locally, then deployed to AWS only when needed, to keep costs low.

1. GitHub Actions runs the test suites and pushes Docker images to GHCR.
2. Terraform provisions an EC2 instance, security group and IAM role.
3. The instance runs the application with Docker Compose.
4. `terraform destroy` tears everything down when the environment is idle.

## Roadmap

The app is built in this order, each step on top of the last:

1. [ ] **Foundations:** project skeleton, database migrations, enforced module boundaries
2. [ ] **Identity:** registration, login, session cookies and CSRF protection
3. [ ] **Projects and monitors:** CRUD with ownership checks on every query
4. [ ] **Monitoring engine:** scheduler, HTTP checker with timeouts, SSRF protection
5. [ ] **Incident detection:** open/resolve state machine, tested against WireMock failure scenarios
6. [ ] **Deployments:** ingest API with hashed per-project API tokens
7. [ ] **Correlation:** link incidents to recent deploys and explain each match
8. [ ] **Dashboard:** latency and uptime charts, incident timeline with deploy markers
9. [ ] **CI/CD:** automated tests and container images on every push
10. [ ] **Cloud deployment:** Terraform-managed AWS infrastructure, with ShipShape reporting its own deploys

## Known limitations

This section will be kept honest as the project develops. Current design trade-offs:

- Single-instance scheduler: checks run inside one application instance, so horizontal scaling would need distributed locking or a dedicated worker.
- Correlation is time-window based and explains its matches; it suggests suspects, it does not prove causation.
- Single-region checks: a network problem between the checker and a service can look like an outage.

## Author

**Thant Zin Min (Tommy)** — Computer Science, Assumption University, Bangkok

- Portfolio: [thantzinmin.tech](https://thantzinmin.tech)
- GitHub: [@Tommyzizii](https://github.com/Tommyzizii)
- LinkedIn: [linkedin.com/in/thantzinmin](https://linkedin.com/in/thantzinmin)