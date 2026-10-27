# Requirements traceability

Source: `NEXUS_Aligned_Complete (1).pptx` found in the user's Downloads directory on 27 October 2026. The original workspace was empty. The prior conversation was readable but exposed reference placeholders rather than the earlier long project prompts. The presentation and current explicit user request therefore define the baseline.

| Requirement | Implementation | Validation / limitation |
|---|---|---|
| Java 21 + Spring Boot backend | Parent Maven build; backend and demo modules | Java compilation/tests; no Python |
| Next.js/React/TypeScript/Tailwind | `frontend/app`, typed client and proxy | Production build; browser fixture tests |
| Real CPU/memory/latency/errors/DB telemetry (slides 3,7) | `Telemetry`, Actuator metrics, Prometheus query | Live stack needs Docker; memory/DB values are context, not learned anomaly models |
| Continuous detection (slides 6,7) | `Detector`, consecutive observation streak | Rule tests and live demo; thresholds are configurable only in code in this version |
| Specialized agent workflow (slides 3,8) | `DiagnosisWorkflow`; seven deterministic roles, three optional Spring AI interpretation roles | Citation/diagnosis tests; live provider checks need key |
| Evidence-based root cause (slides 5,6,10) | immutable evidence IDs, dependency traversal, bounded cited AI output | Semantic correctness remains an evaluation task |
| Historical memory (slide 9) | PostgreSQL summaries, failed attempts/outcome and prevention | Memory persists even if embedding fails |
| RAG/vector search (slides 3,9) | Spring AI embeddings, model-filtered PGvector cosine search/HNSW | Requires API key + Docker/PGvector; labelled lexical fallback |
| Interactive Failure Graph (slide 10) | React Flow graph with service status and incident navigation | Static declared topology; no trace-discovered edges |
| Safety and isolated validation (slides 3,10) | Separate sandbox network/database, fixed action registry | Behavior replica; not equivalent production staging |
| Developer-approved recovery (slides 6,10) | Session/role/CSRF + explicit approval + revision + TTL + state check | Security/recovery tests; no generated command execution |
| Rollback steps (slide 10) | Prior mode retained and manual demo instructions | Faults are never automatically reintroduced |
| Health verification (slides 6,10) | Three application probes with latency bound | Recovery tests and live stack walkthrough |
| Incident documentation (slides 8,9,11) | Audit trail, outcome, historical context, report docs | No external ticketing integration |
| Authentication (current request) | Developer and observer accounts, BCrypt, sessions | MockMvc sign-in/role/CSRF tests; local scope |
| Prometheus, OTel/Jaeger, Loki (current request) | Compose infrastructure, Java agent, collector config | Configured; runtime collector delivery requires Docker |
| Demo services and Docker Compose | Orders/payments/inventory + sandbox | Error/latency/real pool exhaustion scenarios |
| Tests and reproducibility | Maven/JUnit/MockMvc/Testcontainers/Playwright, setup and demo scripts, CI | Distinguish fixture tests from live validation |
| Academic evaluation (slides 6,11) | Measurement protocol, experimental plan, live-demo measurement output | No invented accuracy, MTTR or success results |

## Implementation stages delivered

1. Baseline capture and stack/toolchain selection.
2. Schema, authentication, collection and incident detection.
3. Typed Java role orchestration, bounded Spring AI assistance and historical retrieval.
4. Isolated validation, approval gate, fixed recovery and health verification.
5. Next.js console, dependency graph, evidence browser and recovery UI.
6. Container topology, observability configuration and live demo script.
7. Automated tests, academic report and explicit verification ledger.

Deployment verification is a distinct remaining stage on a Docker-capable host. Enabled AI and vector retrieval need provider credentials. Neither is reported as completed by compiling code or exercising UI fixtures.
