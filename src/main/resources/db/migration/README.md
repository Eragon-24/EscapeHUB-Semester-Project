# Database migrations

Place Flyway migrations here using names such as `V1__create_users_and_teams.sql`.

`V1__create_shared_game_state.sql` stores each session's authoritative room state, shared attributes,
completed puzzles, collected items, exit state, and version. The default `local` profile applies the
migration to in-memory H2; the `postgres` profile applies the same migration to PostgreSQL.
Session metadata and participant membership still use in-memory adapters.
