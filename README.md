# EscapeHub

**Collaborative mystery missions platform with open data.**

This repository contains the initial IntelliJ-ready project skeleton for the Software Engineering semester project.

> Current status: V0.1.0 (US001–US006) is published. The `feature/web-lan-multiplayer-game` branch contains the in-progress browser-based LAN multiplayer conversion.

## Proposed stack

- Java 25 LTS
- Spring Boot 4.1.2
- IntelliJ IDEA
- Maven
- Thymeleaf, HTML, CSS and JavaScript
- PostgreSQL (target database)
- Spring Data JPA / Hibernate
- Flyway
- JUnit, Mockito and Spring Boot Test
- Checkstyle
- GitHub Actions
- Docker / Docker Compose

These are the current project proposals. Confirm the final choices with the team and instructor before treating them as approved decisions.

## Open in IntelliJ IDEA

1. Install a JDK 25 distribution.
2. Extract/clone this repository to a normal local folder.
3. In IntelliJ IDEA choose **File → Open** and select the folder containing `pom.xml` (or open `pom.xml`).
4. When IntelliJ asks, choose **Open as Project** and allow Maven to load dependencies.
5. Open **File → Project Structure → Project** and set Project SDK to JDK 25 and language level to 25.
6. Open `src/main/java/com/escapehub/EscapeHubApplication.java` and run `EscapeHubApplication`.
7. Visit `http://localhost:8080`.

The default `local` profile uses an in-memory H2 database and does not require Docker/PostgreSQL for the initial skeleton.

## Run with PostgreSQL

1. Copy `.env.example` to `.env` and change the local database password.
2. Start the database:

   ```bash
   docker compose up -d db
   ```

3. In IntelliJ open **Run → Edit Configurations**, select `EscapeHubApplication` and add these environment variables (replace the password with the value used in `.env`):

   ```text
   SPRING_PROFILES_ACTIVE=postgres
   DB_URL=jdbc:postgresql://localhost:5432/escapehub
   DB_USERNAME=escapehub
   DB_PASSWORD=your-local-password
   ```

   Note: Docker Compose reads `.env` automatically; IntelliJ does not automatically import that file unless a separate plugin is configured. For this skeleton, set the variables in the Run Configuration as shown.
4. Run the application from IntelliJ.

To stop the database:

```bash
docker compose down
```

To delete the local database volume as well (destructive):

```bash
docker compose down -v
```

## Browser multiplayer demo (LAN)

The browser-based top-down demo is served by Spring Boot. The local profile binds
the server to `0.0.0.0:8080`, allowing other devices on the same trusted LAN to
connect to the host computer.

1. Start `EscapeHubApplication` on the host computer.
2. Open `http://localhost:8080/game` on the host to verify the game.
3. Find the host computer's private IPv4 address (for example with `ipconfig`
   on Windows).
4. On each other device connected to the same Wi-Fi/LAN, open
   `http://<HOST-LAN-IP>:8080/game`.
5. If Windows Firewall prompts, allow Java/EscapeHub on **Private networks**
   or create an inbound TCP rule for port `8080` on the Private profile.

Do not configure router port forwarding for this local-network demo. All players
who connect to this initial demo share one room and one mission state. Each browser
controls its own character; notebook discovery, the chest and the exit are shared.
The server is authoritative for player movement, interaction distance and puzzle
validation. WebSocket communication uses the same host and port as the web page.

This is a local demonstration, not a production deployment. Authentication,
separate game rooms, persistent sessions and internet-facing security are outside
this milestone.

## Initial endpoints

- Landing page: `GET /`
- Browser game: `GET /game`
- Legacy room URL (redirects to the browser game): `GET /room`
- Real-time game channel: `/ws/game` (WebSocket)
- Basic API check: `GET /api/v1/health`
- Spring Boot health: `GET /actuator/health`

## Project structure

```text
src/main/java/com/escapehub/
  config/       shared application configuration
  home/         initial landing page and health endpoint
  user/         accounts and authentication
  team/         teams and memberships
  mission/      mission definitions and catalogue
  gameplay/     mission runs and route exploration
  puzzle/       puzzle rules and validation
  evidence/     evidence journal
  progress/     mission/route/puzzle progress
  integration/  external APIs and snapshots
  common/       shared exceptions and validation
```

Feature modules are organized into `web`, `application`, `domain` and `infrastructure` packages (or focused integration subpackages).

## Build and tests

Use Maven from IntelliJ's Maven tool window, or from a terminal with Maven installed:

```bash
mvn clean verify
```

The verification lifecycle runs tests and Checkstyle. The Checkstyle engine is explicitly pinned to a Java 25-compatible release.

When the puzzle/gameplay rules and their test suite have been implemented, run mutation testing with:

```bash
mvn -Pmutation-testing test-compile org.pitest:pitest-maven:mutationCoverage
```

The HTML report is written under `target/pit-reports/`. The initial skeleton has no gameplay rules yet, so mutation testing is intentionally not part of the default build.

## Important implementation notes

- The security configuration is intentionally permissive for this empty scaffold. It is not production-ready. Implement authentication, authorization and appropriate CSRF behavior before adding real user operations.
- No final database migration is included because the ER model has not yet been approved. The first Flyway migration should be added after the team agrees on entities and relationships.
- Do not put API keys, passwords or `.env` files in Git.
- Keep mission definitions data-driven so another small mission can be added without copying the core gameplay logic.
- Automated tests must use mocks/stubs/fixtures rather than depending on live public APIs.

## Documentation

- [Initial architecture](docs/architecture/ARCHITECTURE.md)
- [Database model draft](docs/architecture/DATABASE_MODEL_DRAFT.md)
- [ADR template](docs/decisions/ADR-TEMPLATE.md)
- [Contribution workflow](CONTRIBUTING.md)
