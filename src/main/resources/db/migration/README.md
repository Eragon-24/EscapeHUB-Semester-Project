# Database migrations

Place Flyway migrations here using names such as `V1__create_users_and_teams.sql`.

The first schema migration is intentionally not included yet: the ER model and entity relationships
must be reviewed and approved before creating production tables. The default `local` profile uses
an in-memory H2 database so the application can be opened and started without PostgreSQL.
Use the `postgres` profile to work against the PostgreSQL service from `compose.yaml`.
