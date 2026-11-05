# ADR 0001: Modular monolith with one AI microservice

**Status:** Accepted

## Context
ReliefGrid has several domains (requests, inventory, tasks, matching, audit). A full microservice split adds deployment, networking and debugging overhead that a small team and a learning-focused project don't need. The AI workload, however, uses a different language (Python) and scales differently.

## Decision
Build the core as a single Spring Boot application with clear module boundaries. Run the AI capability as a separate FastAPI service, connected through Kafka.

## Consequences
- Simpler development, testing and deployment
- Modules can be extracted into services later if needed
- Requires discipline to keep module boundaries clean
