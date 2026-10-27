# Verification ledger

Verified on **27 October 2026 (Asia/Kolkata)** in the initially empty `D:\NEXUS` workspace. This distinguishes source implementation from runtime proof.

| Check | Result | What it establishes |
|---|---|---|
| Java 21 Maven `verify` | PASS | Both modules compile, test and package as executable Spring Boot jars |
| Standard Java tests | **29 passed; 2 skipped; 0 failures** | Diagnosis/citations, detection, HTTP capture, recovery, security, demo controls, OpenRouter serialization and memory guards. The opt-in live AI test and Docker database test are skipped in a normal run. |
| Explicit live Java/Spring AI test | **1 passed; 0 failures** | Actual free Nemotron chat returns cited JSON and actual Nemotron embedding returns 1536 finite values. Executed separately with synthetic data. |
| PGvector Testcontainers integration | SKIPPED | Docker engine is unavailable; database migrations, vector SQL and database concurrency are not runtime-proven here |
| Next.js production build + TypeScript | PASS | Console and same-origin proxy compile successfully |
| Playwright browser tests | **5 passed** | Graph/empty state, explicit approval payload, observer controls, mobile fit and unavailable-backend messaging |
| Desktop/mobile visual inspection | PASS | Reviewed generated fixture screenshots; mobile graph adjusted to a vertical layout |
| Docker Compose v2.40.3 config validation | PASS | Compose syntax, references and environment interpolation are valid |
| Public container image manifests | PASS | All nine referenced image tags exist; this does not prove image startup |
| OpenTelemetry Java agent artifact | HTTP 200 | Version 2.22.0 artifact exists; container agent delivery remains unverified |
| Local secrets setup | PASS | `.env` generated with independent random credentials; no existing credentials overwritten |
| Full Compose startup / live demo | NOT RUN | Requires Docker Desktop/Linux engine |
| Live Prometheus/Loki/Jaeger ingestion | NOT RUN | Requires running containers and generated requests |
| OpenRouter authentication and direct API smoke checks | PASS | Saved key authenticated; one chat and one 1536-dimensional embedding request returned HTTP 200. Credentials were not printed. |
| Enabled Spring AI provider connection | PASS | Actual Java client test passed; no database or deployed application is involved in this test. |
| Live PGvector RAG search | NOT RUN | Provider embedding is verified, but vector persistence/retrieval still needs the PostgreSQL container. |
| Academic performance experiment | NOT RUN | No accuracy, MTTR or recovery-rate results invented |

## Test breakdown

- AI grounding: 2 tests validate three separate Spring AI role outputs against allowed citation IDs, using a mocked model.
- Detector: 2 tests cover consecutive observations and healthy-streak reset.
- Diagnosis: 2 tests cover failed dependency traversal, evidence links and manual fallback on missing evidence.
- Recovery: 6 tests cover missing validation, expiry, stale/replayed approval, failed sandbox, changed live fault and fixed-action success with three health calls. Dependencies are mocked in these tests.
- Security: 8 MockMvc tests cover anonymous rejection, observer restrictions, CSRF, explicit approval confirmation, CSRF access and real password authentication/session creation.
- Telemetry: 2 tests make actual HTTP requests to a local fixture server and verify captured responses, thresholds and unavailable-source handling.
- Demo: 3 tests cover fault responses, held connection release and unknown action rejection with mocked DB handles.
- Database: 1 integration test is prepared for PGvector/Flyway/partial unique index/state revision checks, but skipped on this host.
- Browser: 5 fixture-based UI tests. Their displayed metrics are test fixtures, not measurements of deployed Java services.
- OpenRouter configuration: 2 tests cover URL paths, final request serialization, provider-specific reasoning settings, embedding dimensions and missing-key rejection using a local HTTP fixture.
- Memory guards: 2 tests cover skipping provider requests on cold memory and rejecting incompatible vectors.
- Explicit live provider test: 1 test calls both free Nemotron endpoints through the actual Spring AI client using synthetic text. Normally skipped unless deliberately enabled with `scripts/check-ai.ps1`.

## Fixes discovered during verification

Password hashes needed an explicit BCrypt identifier for Spring Security's delegating encoder. The frontend generic API client needed a correct type for 204 responses. Windows Java NIO failed to connect temporary domain sockets through the inherited short TEMP path; tests now use their normal build directory. Browser assertions were narrowed to the application's alert rather than Next.js's route announcer. The mobile dependency graph was made vertical. The Java container build explicitly makes the downloaded agent readable by its non-root runtime user.

OpenRouter integration tests also caught Spring AI 1.1 dropping the `extraBody` reasoning extension while merging options. A narrow outbound interceptor now applies `reasoning.enabled=false` to NVIDIA `:free` chat requests after serialization. Tests verify this on the wire and verify other model requests are not changed. Nemotron's default 2048-dimensional output does not require a schema change: the live provider accepted the explicit 1536-dimension option. Local AI configuration was enabled after these checks passed.

## Remaining acceptance work

1. Start Docker Desktop with Linux containers. Run `docker compose config --quiet`, `docker compose up --build -d`, and inspect service readiness/logs. The standalone Compose validator downloaded in `.tools` cannot supply a Docker engine.
2. Run Maven verification with Docker available so the PGvector integration test executes rather than skips.
3. Exercise `node scripts/live-demo.mjs`, review the incident and approve it in the console. For a fresh explicitly approved scripted trial use `node scripts/live-demo.mjs --approve`.
4. Confirm actual Prometheus scrapes, trace delivery to Jaeger and log delivery to Loki. Inspect missing-source records if any collector is unavailable.
5. The provider key, free models and embedding compatibility are configured and verified. With the stack running, recover an incident and demonstrate persisted, model-filtered vector retrieval. Record provider/model versions and quotas.
6. Execute the evaluation matrix and fill measured results. Broader fault coverage and production deployment are future work, not completion claims.

## Local toolchain

The preinstalled Java was 8. A workspace-local Temurin Java 21 JDK and Maven 3.9.11 were downloaded into ignored `.tools`, without replacing the system Java. Docker Compose v2.40.3 was downloaded for configuration-only checks. Node 24.16.0 was already available. Chromium was installed for Playwright. These tools are build aids, not part of the Java backend.
