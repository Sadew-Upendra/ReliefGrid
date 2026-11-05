# ADR 0002: PostgreSQL with PostGIS for data and geo queries

**Status:** Accepted

## Context
Matching needs to find the nearest supply points and volunteers, and the map needs bounding-box queries. Hand-written distance math is slow and error-prone.

## Decision
Use PostgreSQL with the PostGIS extension, storing locations as `geography` with spatial indexes.

## Consequences
- Correct, fast geo queries (`ST_DWithin`, `ST_Distance`)
- One database for relational and spatial data
- Local and cloud (RDS) both support PostGIS
