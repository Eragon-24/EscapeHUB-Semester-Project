# Database migrations

Place Flyway migrations here using names such as `V1__create_users_and_teams.sql`.

`V3__create_shared_game_state.sql` stores each session's authoritative room state, shared attributes,
completed puzzles, collected items, exit state, and version in PostgreSQL. The default `local` profile
uses the H2-compatible schema in `db/migration-local` for the JPA game-state tables.
Session metadata and participant membership still use in-memory adapters.
