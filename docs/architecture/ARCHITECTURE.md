# EscapeHub – Initial Architecture

Status: **Proposal / initial project skeleton**. This document is not an approved Architecture Decision Record.

## Architecture style

Proposed Modular Monolith: one Spring Boot application, organized into feature modules with clear boundaries.

## Layers inside a feature module

- `web`: MVC controllers and request/response DTOs.
- `application`: use-case orchestration and application services.
- `domain`: domain rules and domain models.
- `infrastructure`: persistence adapters and external technical integrations.

## Modules

- `user`: accounts and authentication-related application behavior.
- `team`: teams, memberships and creator/member distinction.
- `mission`: mission catalogue and mission definitions.
- `gameplay`: mission runs, route exploration and game actions.
- `puzzle`: puzzle definitions, validation and unlocking rules.
- `evidence`: evidence definitions and collected evidence journal.
- `progress`: route, puzzle and mission progress.
- `integration`: public API clients and stable API snapshots.
- `common`: shared technical concerns only; avoid turning it into a dumping ground.

## Data and infrastructure

- PostgreSQL is the target relational database.
- Spring Data JPA/Hibernate is proposed for persistence.
- Flyway is prepared for schema migrations.
- The default `local` profile uses in-memory H2 to make the skeleton easy to run.
- Use the `postgres` profile with the supplied Docker Compose database for PostgreSQL development.

## External APIs

Wikidata and Open Library are proposed candidate sources, pending confirmation against the final mission theme. External calls must be made server-side, with timeouts, error handling, request limits and stable snapshots/fixtures.

## Security note

The current scaffold intentionally permits requests so the empty project can be opened and previewed. This is not an authentication implementation and is not production-ready. Implement and test authorization before real user data or gameplay is added.

## Not implemented yet

No user registration, team creation, mission definitions, puzzle engine, evidence persistence, API clients or final database schema are implemented in this scaffold.
