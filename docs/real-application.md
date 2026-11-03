# Real application extension

NEXUS Tasks is a separately deployed Java 21/Spring Boot application backed by its own PostgreSQL database. It is the first actual application monitored by NEXUS. It stores user-created work; the monitoring console does not invent projects, incidents or successful actions.

## Start and use

1. Install Docker with Linux containers and Compose v2. Allocate around 8 GB RAM. Node is required only for the credential setup helper when building everything in containers.
2. From the repository run `node scripts/setup.mjs`. Existing `.env` values remain intact; missing application passwords and token are generated without printing them.
3. Stop the original demo stack if running: `docker compose down` (do not use `-v` when preserving its data).
4. Run `docker compose -f compose.application.yaml up --build -d`.
5. Open `http://localhost:3000/work`. Sign in as `admin` using `WORK_ADMIN_PASSWORD` stored locally in `.env`. The first startup provisions this account; changing that environment value later does not reset an existing database password.
6. Create member accounts under Administration. Create a project, add members by username, and create/assign/update tasks. Members see only their projects; administrators see all. Archive a project to prevent further task writes. Conflicting edits return 409 and require a refresh instead of overwriting another person's work.
7. Sign in separately to `http://localhost:3000` as `developer` using `NEXUS_PASSWORD`, or `observer` using `OBSERVER_PASSWORD`. The sessions and CSRF cookies of the two applications are distinct. The Tasks session is process-local; signing in again is required after a Tasks service restart. Data and accounts survive in PostgreSQL.

## Monitored operations and recovery

The real application has an administrator-controlled maintenance setting with a reason, persisted revision and audit record. Enabling it blocks project/task routes with 503 while authentication and administration remain available. This is an explicit operational action, not a hidden synthetic fault. It can represent real planned maintenance; do not resume it until that maintenance is complete.

NEXUS observes the token-protected readiness probe, operational state, bounded recent events, Micrometer metrics, Prometheus, Loki and Jaeger. Two anomalous observations open an incident. It cites captured evidence and consults historical memory. With a compatible AI provider enabled, the bounded read-only Spring AI roles also review the evidence. AI cannot invent an executable action.

The only automated runbook for this application is `RESUME_TRAFFIC`. It first reproduces maintenance and verifies recovery using the same application build against an isolated database. Business records are never copied into that environment. Validation records the real application's operational revision and expires after five minutes. Developer approval claims the incident revision once; a conditional database update rejects any operational change since validation. Recovery requires three successful database-backed probes. Failure stays open and is audited; NEXUS does not reverse business data or execute shell commands. An administrator can resume/restore maintenance directly with a reason when needed.

Unavailable database/service/telemetry or unsupported causes must lead to missing evidence or manual investigation, never an invented diagnosis or arbitrary recovery. The current real target is one application service; the original three-service laboratory remains for dependency failure scenarios. This is not an arbitrary service-discovery or Kubernetes operator product.

## Verification

`mvn verify` runs unit/security checks and, when Docker is available, PostgreSQL integration tests. The cloud test summary explicitly fails if either real PostgreSQL suite skipped. The test client declares Docker API 1.44 to support current engines. `npm run build` and `npm test` in `frontend` validate the build and existing console fixtures.

The full workflow additionally starts `compose.application.yaml` with prebuilt artifacts, runs `node scripts/application-acceptance.mjs`, and executes `playwright.application.config.ts`. The acceptance script requires `NEXUS_DISPOSABLE_TESTS=true`: it stops a database and creates temporary restoration/crash fixtures, so run it only on throwaway infrastructure. It checks real sign-in/roles/CSRF, unauthorized membership, assignment, conflicting edits, application restart persistence, changed-state approval rejection, sandbox validation, successful one-use approval, post-action health, memory, nonempty collector data, archived writes, an actual database outage/reconnect, restored backup data, interrupted-action handling and browser-created records. Evidence is uploaded as the workflow artifact; results must be read from the actual run, not inferred from compilation.

A push explicitly marked `[application-ai]` enables one additional free-provider scenario using the repository secret. It requires three accepted cited Spring AI roles, actual historical vector context, approved recovery and retrieval of the new outcome through PGvector. It fails if the provider falls back; ordinary offline tests still verify fallback behavior without claiming a live model success. Provider output remains labelled as interpretation, since valid citation identities alone do not prove semantic correctness.

These are bounded acceptance checks, not a promise that all possible failures are handled. Provider outages fall back to deterministic evidence, process-local sessions need sign-in after restart, and this single-host deployment has no high availability guarantee.

## Data and operation

Stop with `docker compose -f compose.application.yaml down`. Named volumes retain both business and incident data. `down -v` deletes them and is used only for disposable CI tests.

Back up separately: `docker compose -f compose.application.yaml exec -T tasks-db pg_dump -U tasks -Fc tasks > tasks.dump` and the equivalent `postgres` service for the `nexus` database. On Windows use a binary-safe output mechanism for custom-format dumps (PowerShell 7.4+ native redirection); verify a restore into a disposable database before relying on a backup. Never commit dumps or credentials. A backup is not verified until restored and checked.

All published ports bind to localhost. Database and recovery networks are private; the sandbox cannot reach the business database. No paid hosting is configured. Continuous operation with the laptop off requires an always-on host; ephemeral cloud verification runners are not permanent hosting. A public deployment additionally needs a domain/TLS reverse proxy, secure-cookie settings, backup scheduling, retention, access governance and capacity testing for its actual workload.
