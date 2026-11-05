# ADR 0003: Asynchronous AI triage through Kafka

**Status:** Accepted

## Context
Citizens submit reports during emergencies. A slow or failing ML model must never block or fail a submission.

## Decision
Save the report immediately with default urgency, publish `request.created`, and let the AI service consume it and publish `ai.classification-result`. The API then updates urgency and need type.

## Consequences
- Fast, reliable submission independent of the AI service
- Eventual consistency: urgency may update seconds after creation
- Coordinators can always override AI results manually
