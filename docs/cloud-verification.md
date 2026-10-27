# Cloud verification procedure

The private GitHub repository is the cloud handoff source. The Windows workspace is not needed. GitHub Actions uses one standard Ubuntu runner, a 25-minute job deadline and concurrency cancellation. It does not provision hosting, change billing settings or enable overages. Runs are ephemeral and their containers and volumes are removed. They do not provide a continuously available application.

## Reproduce without a Windows toolchain

On a Docker-capable Linux host with Node 24:

```sh
node scripts/setup.mjs
docker compose config --quiet
docker compose up --build -d
node scripts/cloud-acceptance.mjs
cd frontend
npm ci
npx playwright install --with-deps chromium
npx playwright test --config playwright.live.config.ts
cd ..
docker compose down -v --remove-orphans
```

The final command deletes the ephemeral test database. Use a separate checkout and Compose project for evaluation, never an existing deployment containing wanted incident records. The setup script preserves an existing `.env`, generates independent random credentials with restrictive permissions and prints none of them.

`cloud-acceptance.mjs` sends real requests through Next.js to Spring Security and the Java backend. It requires live application probes, root-service diagnosis, sandbox fault reproduction/reset, explicit scripted developer approval, verified recovery, audit records, historical retrieval and nonempty Prometheus/Loki/Jaeger results. It checks anonymous access, observer mutations, missing CSRF, missing validation, unconfirmed approval, stale revision and replay rejection. Invalid responses fail the job; no response is replaced by fixtures.

The developer's authorization for this isolated acceptance run supplies approval for the predefined RESET_FAULT demo action. This does not authorize autonomous production recovery. The UI continues to require its explicit developer approval flow.

Evidence contains incident IDs, timing and assertions only. Browser screenshots are captured after sign-in; browser traces are disabled to avoid retaining submitted passwords. Raw `.env`, authentication requests, cookies and provider keys must not be uploaded. CI artifacts expire after three days; copy measured summaries into versioned documentation after review.

The full script now checks expiry and changed live-state rejection as well. The final optional AI scenario separately established vector persistence/retrieval and accepted cited interpretations. These checks do not establish semantic AI quality, distributed races or the full repeated evaluation matrix. See the verification ledger for measured runs. Provider tests remain deliberately opt-in and must use the encrypted Actions secret without logging it.

## First runtime defect

The first Docker-enabled Maven run reached the PostgreSQL integration test and failed during application-ready reconciliation: direct package-field access to a Spring exception-translation repository proxy returned an uninitialized store collaborator. `IncidentStore` is now a regular Spring component; its explicit `JdbcTemplate` and `TransactionTemplate` retain SQL exception translation and execution transaction behavior. The existing real database startup test is the regression check. A mocked store could not establish this property.

## Bounded AI scenario

The optional AI scenario uses the existing disposable database and retains one real resolved historical record before embedding it. It then injects ERROR in orders (the upstream service, so no downstream fault propagation creates extra model investigations). Exactly one incident is intended: three cited Spring AI interpretations, PGvector historical context, sandbox validation, approved recovery, a persisted new embedding and a subsequent vector query. The nominal provider request count is seven: three chat calls and four embeddings. Provider/framework retries and failures must be reported; a labelled fallback is not a passing AI result. Only synthetic demo telemetry is sent.

## Host port correction

The original Compose attached every published UI/collector service only to internal networks. The first complete container build succeeded, but the host API client could not connect to port 3000. Published user interfaces now have explicit bridge edge networks; all published ports remain bound to 127.0.0.1. Demo, control and sandbox networks remain internal, with the sandbox database on its own isolated network. This change does not publish backend, database, demo-control or OTLP ports. CI can use `compose.ci.yaml` to package the exact Java jars and Next.js standalone output already built/tested in the same job, avoiding duplicate compilation. The normal source-building Dockerfiles remain available.

## Final result

Run [40564112317](https://github.com/rajeevreddy23/NEXUS/actions/runs/40564112317) passed. Its targeted AI scope repeated a fresh seed recovery and collector checks, the populated browser graph, and one three-role Spring AI/RAG/approved-recovery scenario. Full three-fault and negative-approval evidence is retained from run 40561946483. The model-output failure and fix are recorded rather than erased. Use a full unmarked push or workflow_dispatch for the complete deterministic suite; `[ai-only]` is an explicit targeted check mode, and `[nexus-ai]` enables the bounded secret-backed scenario only on a main-branch push. It is not enabled on pull requests.
