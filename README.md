# NEXUS

An academic reliability platform for **root-cause diagnosis and developer-approved recovery** in a small cloud-native demo. The baseline is the supplied Mohan Babu University presentation, preserved in [docs/NEXUS-baseline.pptx](docs/NEXUS-baseline.pptx) and extracted in [docs/presentation-baseline.md](docs/presentation-baseline.md).

**Frontend:** Next.js, React, TypeScript, Tailwind, React Flow. **Backend and demo services:** Java 21, Spring Boot 3.5.7. **AI:** Spring AI 1.1.0 with bounded Java role orchestration. **Storage:** PostgreSQL 17 + PGvector. **Observability:** Micrometer/Prometheus, OpenTelemetry Java agent/Collector, Jaeger, Loki. No Python backend or Python tooling is needed.

## Verified cloud demonstration

The [final cloud workflow passed](https://github.com/rajeevreddy23/NEXUS/actions/runs/40564112317): actual Compose deployment, authenticated populated dependency graph, real telemetry, approved recovery, and one Spring AI/PGvector scenario. A preceding full acceptance run covered ERROR, LATENCY, real DB_POOL exhaustion and negative approval cases. See [measured verification](docs/verification.md), [evaluation](docs/evaluation.md), [evidence](docs/evidence/README.md) and [remaining work](docs/remaining-work.md). Tests used ephemeral runners; no continuously hosted URL or paid infrastructure was created.

## Quick start on Windows

Prerequisites: Docker Desktop with Linux containers and Docker Compose v2. Allow about 8 GB RAM for Docker and several GB disk space. Node 24 and Java 21/Maven 3.9 are needed only for local development or tests outside containers.

```powershell
cd D:\NEXUS
pwsh -File scripts/setup.ps1
docker compose config --quiet
docker compose up --build -d
docker compose ps
```

Open **[NEXUS](http://localhost:3000)**. Sign in as `developer` with `NEXUS_PASSWORD` from your local `.env`. The read-only account is `observer`, using `OBSERVER_PASSWORD`. Setup generates distinct random credentials and preserves an existing `.env`; it never prints secrets.

First startup downloads images, compiles Java and builds Next.js. Give the services time to become ready, then refresh. There are no invented incident records or sample success rates. The dashboard fills from actual probes and telemetry. If the database fails, sign-in or API requests will fail rather than silently switch to a fake backend.

Stop with `docker compose down`. Database and telemetry volumes remain. Back up incident data with `docker compose exec -T postgres pg_dump -U nexus nexus` before changing persistent storage.

## Cloud and Linux verification

The private repository includes a bounded GitHub Actions workflow that runs Java/PGvector checks, the frontend suite, the complete isolated Compose stack and real API/browser acceptance. See [cloud verification](docs/cloud-verification.md) and the [verification ledger](docs/verification.md) for measured status. A workflow definition or queued job is not deployment proof. GitHub runners are ephemeral; no permanent hosting is provisioned.

On Linux, `node scripts/setup.mjs` generates a private `.env` without printing credentials. Then run `docker compose up --build -d` and `node scripts/cloud-acceptance.mjs` in a disposable checkout.

## Demonstrate the full cycle

1. Wait for all three services to pass probes in Overview.
2. Open Demo lab. Inject an `ERROR` into `inventory`.
3. Wait for two consecutive anomalous scans, or collect telemetry twice. Orders calls payments, which calls inventory, so the failure propagates.
4. Open the inventory incident. Inspect findings and evidence IDs. Unavailable collectors are explicitly labelled.
5. Run sandbox validation. A replica using a **separate database and internal network** must reproduce the fault and then pass three probes after resetting it.
6. Review the proposed `RESET_FAULT` action and target. Confirm the approval checkbox, then approve and execute. The backend requires a developer session, CSRF token, matching incident revision, validation under five minutes old, and the same live fault mode.
7. Confirm `RESOLVED`, examine the audit trail, and search Incident memory for `inventory ERROR`.
8. The related orders/payments incident records remain for review; close those with notes once their probes pass. Repeat a failure to demonstrate historical retrieval.

Other supported scenarios are `LATENCY` (actual 1.6-second delay) and `DB_POOL` (actual connection handles held until a three-connection Hikari pool is exhausted). `NONE` clears a demo fault. These are controlled demo controls, not general infrastructure repair.

For a real API walkthrough, run `node scripts/live-demo.mjs`. It stops after sandbox validation. For a fresh demo, `node scripts/live-demo.mjs --approve` explicitly approves the fixed local demo recovery and writes actual measurements to `docs/latest-demo-result.json`. Start with a healthy environment and no old open inventory incident. The script does not invent measurements.

## AI and historical retrieval

Default `AI_ENABLED=false` runs seven deterministic analysis/planning roles and PostgreSQL full-text memory retrieval. The UI labels that mode. This supports an offline academic demonstration but **does not demonstrate LLM reasoning or vector retrieval**.

To enable the AI path, edit `.env`:

```dotenv
AI_ENABLED=true
OPENAI_API_KEY=your-provider-key
CHAT_MODEL=nvidia/nemotron-3-ultra-550b-a55b:free
EMBEDDING_MODEL=nvidia/nemotron-3.5-embed-1b:free
AI_BASE_URL=https://openrouter.ai/api
```

The defaults use OpenRouter's free Nemotron chat and embedding models. The Spring AI base URL is `https://openrouter.ai/api` because the client appends `/v1/chat/completions` or `/v1/embeddings`; do not append another `/v1`. Nemotron embedding requests explicitly ask for **1536 dimensions**, which the live endpoint successfully returned, matching the existing PGvector schema without dropping data or migrating the column. Free Nemotron chat requests disable extra reasoning so the short JSON response budget is available for findings. Model availability and rate limits depend on the provider. Restart the backend: `docker compose up -d --force-recreate backend`.

Validate the Java provider connection independently of Docker using `pwsh -File scripts/check-ai.ps1`. It reads the key privately from `.env` and sends two small synthetic requests: one cited JSON response and one embedding. Normal automated tests never use your key or call a live provider. A cold incident-memory database also skips unnecessary embedding requests. OpenRouter free quotas are shared across model calls, so multi-agent investigations can consume the daily allowance quickly. These free NVIDIA endpoints can log inputs; use isolated demo data.

Three distinct Spring AI roles (log analysis, dependency analysis, root-cause review) exchange bounded findings within the Java workflow. Structured model responses must cite existing, available evidence IDs. Citation validation cannot establish that an explanation is correct, so the UI labels model conclusions as unverified interpretations. AI cannot select an executable action, access a shell, approve recovery or bypass safety checks. Provider failures produce a labelled fallback. Incident memory stores resolved and closed outcomes, including failed recovery attempts, and uses PGvector cosine similarity when embeddings are available. Use **Generate missing embeddings** after enabling AI for existing records. Vectors are filtered by embedding model; a dimension change requires a deliberate schema migration.

## Observability

| Tool | Local URL | Purpose |
|---|---|---|
| NEXUS | [localhost:3000](http://localhost:3000) | Evidence, incidents, approval and memory |
| Prometheus | [localhost:9090](http://localhost:9090) | Scraped Micrometer metrics |
| Jaeger | [localhost:16686](http://localhost:16686) | Real HTTP/JDBC trace spans |
| Loki | [localhost:3100/ready](http://localhost:3100/ready) | OTLP-exported application logs |

Demo services, databases, backend control endpoints and OTLP ports are not exposed on the host by Compose. Published ports bind to loopback. Java agents emit traces/logs to the Collector; traces go to Jaeger and logs to Loki. Micrometer metrics are scraped independently by Prometheus. The dashboard's service metrics come from actual Actuator endpoints; the HTTP error-rate query comes from Prometheus. Trace/log query responses are retained as evidence. The dependency graph uses the declared service topology, not automatically inferred trace topology.

## Development and testing

```powershell
# Optional local toolchain already downloaded under .tools on the initial build machine:
$env:JAVA_HOME = (Get-ChildItem .tools -Directory -Filter 'jdk-21*' | Select-Object -First 1).FullName
.\.tools\apache-maven-3.9.11\bin\mvn.cmd -B verify

cd frontend
npm ci
npm run build
npx playwright install chromium
npm test
npm run dev
```

On another machine use an installed Java 21 and `mvn`. Java tests place temporary domain sockets in each module's build directory to avoid Windows short-path socket failures. The Java build contains security, diagnosis, recovery and demo tests. The PGvector integration test needs Docker and is explicitly skipped when absent. Browser tests use labelled network fixtures to test UI behavior; they do not prove real telemetry or live recovery. `scripts/test.ps1` runs the Java checks, frontend build and browser suite after the browser is installed.

For local frontend development against the Compose backend, run the frontend in Compose or deliberately expose the backend on loopback with a personal Compose override; the default local proxy expects `http://127.0.0.1:8080`. The default Compose frontend uses the internal backend address automatically.

See [architecture](docs/architecture.md), [requirements traceability](docs/requirements.md), [academic report](docs/academic-report.md), [evaluation protocol](docs/evaluation.md), [API](docs/api.md), and [verification status](docs/verification.md).

## Scope and limits

This implementation targets one controller, one developer account, one observer account, and an isolated three-service demonstration. Runtime recovery is limited to clearing controlled demo faults. It does not restart arbitrary containers, execute generated commands, scale services, repair application code, or operate a production cluster. Sandbox tests establish behavior in a simplified replica; they do not prove that production recovery will work. There is no automatic rollback that reintroduces a failure. The prior fault mode and manual demo rollback instructions are retained for inspection.

Sessions reside in the backend process and expire on restart. AI model calls are remote when enabled. Controller restarts during execution mark the outcome uncertain; actions are never replayed. Long-running validation/execution occurs synchronously and is bounded for this local demo. Multi-controller deployment, distributed execution leases, SSO, secret rotation, immutable external audit storage and production sandbox equivalence require further work.

