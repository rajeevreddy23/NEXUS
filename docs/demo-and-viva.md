# Live demo and viva guide

## Ten-minute walkthrough

**Minute 0–2:** Show the three Java services and the orders → payments → inventory graph. Explain the distinction between application work probes and process liveness. Open Prometheus targets and Jaeger; demonstrate at least one recent real request trace and verify Loki ingestion before claiming the stack is healthy.

**Minute 2–4:** Inject an inventory ERROR. Show propagation, two-observation detection, the incident's captured evidence and the probable root dependency. Click a finding's evidence link and inspect timestamp/source/contents.

**Minute 4–6:** Show deterministic mode versus AI-assisted mode honestly. Demonstrate historical context if prior records exist; an empty history is a valid cold start. Explain that a valid citation ID does not prove a model statement is correct.

**Minute 6–8:** Run the isolated sandbox test. Explain its separate database and network and its limited topology. Review action, target and rollback description. Approve the incident revision explicitly and show the three successful live checks in the audit.

**Minute 8–10:** Show the outcome in incident memory. Explain why related incident records remain for developer review, then close them with notes. Describe evaluation metrics and the outstanding experiments without inventing results.

## Questions to prepare

- **Why Java?** The backend, workflow and demo use the requested Java 21/Spring Boot stack. Spring AI provides chat and embedding abstractions without a Python agent server.
- **Is recovery autonomous?** Detection and investigation are automatic; executable recovery is restricted to predefined demo actions after isolated validation and explicit developer approval.
- **What makes it multi-agent?** Typed specialist roles exchange findings in a bounded Java workflow. When AI is enabled, log, dependency and root-cause interpretation use separate Spring AI calls. Without a key, the roles use deterministic logic and the UI labels this clearly.
- **How is RAG implemented?** Incident outcomes become summaries, Spring AI embeds them, PGvector ranks model-compatible vectors by cosine similarity, and retrieved text enters the diagnostic context. Without embeddings, the fallback is PostgreSQL full-text retrieval.
- **What prevents dangerous commands?** There is no command execution surface or Docker socket. The action registry accepts only RESET_FAULT on three registered demo services; model output never becomes an action.
- **What does the sandbox prove?** That the same local fault behavior can be reproduced and cleared in a separate replica. It does not prove complete production equivalence.
- **What happens after a crash?** Sessions expire; interrupted actions become uncertain/failed records and are never replayed. A developer must inspect current health.
- **What are the main weaknesses?** Known injected modes, static topology, fixed thresholds, single controller, simplified sandbox and unmeasured real-world diagnosis quality.

## Troubleshooting

**Sign-in fails:** Confirm the backend started, PostgreSQL is healthy and the password matches `.env`. Do not use Java 8. Check `docker compose logs backend` without copying credentials into reports.

**All services show down:** Check demo-db readiness and service logs. First-start readiness can create early incidents; close those with a note once services pass application probes.

**No incidents after one scan:** Two consecutive anomalous observations are required. Scan again or wait for scheduled collection to complete.

**Plan is manual investigation:** The system lacks evidence of an allowlisted demo fault. Inspect state/probe availability. Clearing unknown faults is not enabled.

**Validation expires:** Click Run validation again. Approval cannot bypass freshness or revision checks.

**Memory is empty:** Recover or close a diagnosed incident first, then search specific terms. A record without a successful embedding uses lexical retrieval until reindexed.

**Loki/Jaeger missing:** Inspect Collector exporter logs and OTLP agent configuration. Missing telemetry must remain labelled unavailable; do not substitute UI fixtures for the live demonstration.
