# ReliefGrid Architecture

## 1. Overview
A **modular monolith** (Spring Boot) plus one **AI microservice** (FastAPI), connected through **Kafka**. PostgreSQL with PostGIS stores data; Redis handles caching and rate limiting.

## 2. System Diagram
```mermaid
flowchart LR
  U[Users: citizen, volunteer, NGO, coordinator, admin] --> W[Next.js Web App]
  W -->|HTTPS + JWT| G[Ingress / API Gateway]
  G --> A[Spring Boot API]
  A --> P[(PostgreSQL + PostGIS)]
  A --> R[(Redis)]
  A -->|produce events| K{{Kafka}}
  K --> AI[AI Service - FastAPI]
  AI -->|classification result| K
  K --> N[Notification consumer]
  K --> A
  A -. metrics .-> M[Prometheus]
  M --> Gf[Grafana]
```

## 3. Backend Modules
| Module | Responsibility |
|---|---|
| auth | Registration, login, JWT and refresh tokens, RBAC |
| requests | Relief requests: type, quantity, urgency, location, status |
| inventory | Supply points, inventory items, pledges |
| tasks | Volunteer task assignment and completion |
| matching | Nearest supply and volunteer suggestions (PostGIS) |
| audit | Immutable log of every state change |
| notifications | Email/SMS dispatch from Kafka events |

## 4. Request Lifecycle
```mermaid
sequenceDiagram
  participant C as Citizen
  participant API as Spring Boot API
  participant DB as PostgreSQL
  participant K as Kafka
  participant AI as AI Service
  C->>API: POST /requests
  API->>DB: save (status = PENDING)
  API->>K: request.created
  API-->>C: 201 Created
  K->>AI: consume request.created
  AI->>K: ai.classification-result
  K->>API: consume result
  API->>DB: update urgency and need type
  API->>K: request.status-changed
```

## 5. Data Model
```mermaid
erDiagram
  USER ||--o{ RELIEF_REQUEST : reports
  USER ||--o{ SUPPLY_POINT : owns
  USER ||--o{ PLEDGE : makes
  USER ||--o{ TASK : volunteers_for
  USER ||--o{ AUDIT_LOG : performs
  USER ||--o{ NOTIFICATION : receives
  SUPPLY_POINT ||--o{ INVENTORY_ITEM : holds
  RELIEF_REQUEST ||--o{ TASK : generates

  USER {
    uuid id PK
    string name
    string email UK
    string password_hash
    enum role
    timestamp created_at
  }
  RELIEF_REQUEST {
    uuid id PK
    uuid reporter_id FK
    enum need_type
    int quantity
    enum urgency
    enum status
    geography location
    text description
    timestamp created_at
  }
  SUPPLY_POINT {
    uuid id PK
    uuid owner_id FK
    string name
    geography location
  }
  INVENTORY_ITEM {
    uuid id PK
    uuid supply_point_id FK
    enum need_type
    int quantity_available
  }
  PLEDGE {
    uuid id PK
    uuid donor_id FK
    enum need_type
    int quantity
    enum status
  }
  TASK {
    uuid id PK
    uuid request_id FK
    uuid volunteer_id FK
    enum status
    timestamp assigned_at
    timestamp completed_at
  }
  AUDIT_LOG {
    uuid id PK
    uuid actor_id FK
    string action
    string entity_type
    uuid entity_id
    jsonb metadata
    timestamp created_at
  }
  NOTIFICATION {
    uuid id PK
    uuid user_id FK
    enum channel
    text message
    timestamp sent_at
  }
```

**Enums**
- `role`: CITIZEN, VOLUNTEER, NGO, COORDINATOR, ADMIN
- `need_type`: FOOD, WATER, MEDICINE, SHELTER, RESCUE, OTHER
- `urgency`: LOW, MEDIUM, HIGH, CRITICAL
- `request status`: PENDING, VERIFIED, ASSIGNED, IN_PROGRESS, FULFILLED, REJECTED
- `task status`: OPEN, ACCEPTED, COMPLETED, CANCELLED

## 6. Kafka Topics
| Topic | Producer | Consumers |
|---|---|---|
| request.created | API | AI service, analytics |
| ai.classification-result | AI service | API |
| request.status-changed | API | Notifications, analytics |
| task.assigned | API | Notifications |

## 7. Security Summary
- JWT access tokens (short-lived) plus refresh tokens
- Role-based access control on every endpoint
- Input validation, rate limiting (Redis), CORS allow-list
- Passwords hashed with BCrypt or Argon2
- Audit log for all state changes
- Secrets via environment/secret manager, never in the repo

## 8. Non-Functional Targets
- Map updates visible in under 2 seconds
- Report submission never blocked by AI (async design)
- Everything containerized and reproducible from `docker compose up`
- API documented, core logic unit-tested, key flows integration-tested
