# Free online evaluation

This profile is a limited evaluation on Vercel Hobby and Render Free. It is separate from the complete Docker application verified in `docs/verification.md`.

## Provisioned resources

- Render workspace: Rajeev Reddy's workspace, Singapore region.
- Database: `nexus-evaluation-db`, ID `dpg-dbl9l20u01pc739i6t80-a`, PostgreSQL 17, free plan. Render reports expiry on **4 December 2026**. Back up before expiry; this is not permanent storage.
- Vercel project: `nexus-console`, ID `prj_h40FgfQLOdhJAAHG1a4ak71ztqMo`, team `rajeev-s-projects3`, Hobby. The frontend directory is linked locally. Project creation does not mean a deployment is live.

## Intended runtime

The Next.js frontend runs on Vercel and uses its server-side proxies to reach two Render Java services. Each Render service requires the same random gateway token; direct API access without that token is denied before login handling. Internal observation endpoints additionally retain the existing operations-token authorization. Liveness remains public. No credentials are sent to the browser bundle.

The one free Postgres instance contains separate `nexus` and `tasks` schemas, each with its own Flyway history. This is not the full deployment's database isolation. Recovery is disabled by the Java API and hidden in the UI. The sandbox, Prometheus, Loki, Jaeger and OpenTelemetry Collector are not provisioned on this profile. Missing external collectors are labelled unavailable. Manual collection still obtains real application readiness, JVM/process/connection metrics and recent events. No fake values or outcomes replace missing collectors.

Free Render services can sleep after 15 idle minutes, and first requests may take a minute to wake them. Automatic polling is disabled to avoid keeping free services awake indefinitely and consuming the workspace's shared free-hour allowance. Use the console's evaluation banner to collect telemetry manually. The proxy permits longer requests within Vercel Hobby's current function limit.

## Required service settings

For both services use `plan=free`, region `singapore`, repository `https://github.com/rajeevreddy23/NEXUS`, and branch `main`.

| Setting | Control service | Tasks service |
|---|---|---|
| Dockerfile | `infra/Dockerfile.render.backend` | `infra/Dockerfile.render.workload` |
| `RENDER_DB_SCHEMA` | `nexus` | `tasks` |
| `RENDER_DATABASE_URL` | Internal URL from the evaluation database | Same database URL |
| `NEXUS_GATEWAY_TOKEN` | Saved random token | Same random token |
| `COOKIE_SECURE` | `true` | `true` |
| `NEXUS_MODE` | `application` | — |
| `EVALUATION_ENABLED` | `true` | — |
| `DEMO_ENABLED` | `false` | — |
| `COLLECTORS_ENABLED` | `false` | — |
| `EXTERNAL_TELEMETRY_ENABLED` | `false` | — |
| `TELEMETRY_TIMEOUT_SECONDS` | `90` | — |
| `SERVICE_ORIGIN` | Public HTTPS URL of Tasks | — |
| Credentials | Existing developer, observer and operations settings | Existing admin and operations settings |

Pass the existing model settings and key to the control service only if enabling AI. The provider remains optional and model failures must be labelled. Set `BACKEND_URL`, `WORKLOAD_URL`, and the server-only `NEXUS_GATEWAY_TOKEN` on Vercel Production and Preview. Set framework Next.js and Node 24.x. Keep environment files, pulled OIDC tokens and deployment credentials out of Git.

## Connection setup

Connection setup is complete. The database URL and application credentials are configured in the Render services; Vercel holds only the backend addresses and server-side gateway secret. Local copies remain in the ignored `.env` file. No credentials are included in Git or evidence.

## Online evaluation addresses

- Application: `https://nexus-console-rajeev23.vercel.app/work`
- Control room: `https://nexus-console-rajeev23.vercel.app/`
- Protected Tasks service: `https://nexus-tasks-23.onrender.com` (`srv-dbla826417fc73fus480`).
- Protected control service: `https://nexus-control-23.onrender.com` (`srv-dbla89e417fc73fusrj0`).

The connection has been saved and both Java services have started against their separate schemas. The Vercel project uses Next.js, Node 24.x, and Singapore functions. Its stable hostname is a project domain and follows production. The generated deployment URL remains protected by Vercel Authentication. Existing local `WORK_ADMIN_PASSWORD` and `NEXUS_PASSWORD` are the application and control-room passwords respectively; they are not committed here.

Do not publish a frontend-only success claim. After services are live, verify gateway rejection, actual sign-in/CSRF, account authorization, project/task creation and reload persistence, manual telemetry, unavailable-collector labels and recovery rejection. Record the real URLs and results here only after those checks pass.

## Verified online results — 4 November 2026

All of those checks passed against the public Vercel hostname and real Render services. Two Playwright browser tests also passed, including creating a project/task and retaining it after reload. An initial public-network connect timeout was fixed by increasing Java's connection allowance; the final readiness probe passed in approximately 127 ms, with no active anomalies and maintenance disabled.

A labelled, temporary administrator maintenance action was detected and reviewed by all three live Spring AI roles. Automated recovery was correctly rejected. The administrator resumed traffic, the incident was closed with the actual outcome, and that outcome was retrieved through PGvector. Normal business access and health were verified afterward. See [saved evidence](evidence/hosting-2026-11-04/provenance.json). Eight focused Java hosting/telemetry checks and five local UI fixtures also passed. No load or high-availability claim is made.

The database accepts internal Render connections; a direct external database query through the hosted connector was blocked by its allowlist. The allowlist was not weakened. Persistence evidence comes from actual API reads, audit records and real browser reloads against the Java application's PostgreSQL storage.
