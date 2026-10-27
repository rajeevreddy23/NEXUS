# NEXUS: An Autonomous Agentic Platform for Root-Cause Diagnosis and Recovery in Cloud-Native Systems

**Department:** Computer Science and Engineering, School of Computing, Mohan Babu University. **Academic year:** 2026–2027. **Batch:** A1-18. **Guide:** Mr. E. Vimal Raj, Assistant Professor, CSE.

**Team (from supplied presentation):** C Tejasri — 23102A040011; Nainika Kandra — 23102A040005; Kondamchetty Kishan Sai — 23102A040075; E Gowtham Kumar — 23102A040049.

## Abstract

Cloud-native applications distribute behavior across services and shared infrastructure. A single failure can produce symptoms in several services, while useful diagnostic information remains divided among metrics, logs and traces. NEXUS implements a small incident-management platform that captures these observations, connects them to service dependencies and records an explainable investigation. Specialized Java workflow roles perform evidence assessment, historical retrieval, probable-cause analysis, recovery planning, safety evaluation and documentation. Optional Spring AI roles add cited interpretations of bounded evidence. PostgreSQL retains incident records and PGvector supports semantic retrieval when model embeddings are available. A developer must approve a predefined recovery after it has been tested in an isolated behavior replica. Application probes verify the outcome and incident memory retains the attempt. The implementation provides a reproducible three-service experimental environment; reductions in diagnosis time or improvements in accuracy must be established by the documented evaluation rather than assumed.

## Problem statement

Small development teams may lack dedicated reliability engineers. During an incident they must correlate observations from separate tools, infer which failing dependency is responsible, choose a repair and verify whether it helped. Previous incident knowledge may be incomplete or difficult to search. An AI recommendation can appear convincing while lacking evidence or being inappropriate for the environment. The project asks whether an integrated evidence and memory workflow can support diagnosis and controlled recovery while preserving a developer's decision.

## Objectives and scope

The implementation monitors a fixed three-service Java application, opens incidents from repeated anomalous observations, retrieves historical records, produces cited findings and plans, tests supported actions in isolation, requires approval and verifies recovery. It supports error responses, latency and connection-pool exhaustion. It retains attempted fixes, outcomes, and suggested preventive actions.

The implementation does not claim unrestricted autonomous repair. The recovery policy is deliberately narrow and deterministic. Automatic cluster discovery, learned anomaly models, source-code patches, container restart/scaling, cloud administration and enterprise identity management are outside this deliverable. Resource measurements are actual application signals; broad production failure coverage is not implied by the three injected scenarios.

## Background and related work

The supplied presentation cites work on multimodal diagnosis, LLM-based root-cause recommendations, multi-agent reliability engineering and evaluation environments. Those references motivate comparing evidence sources, preserving historical knowledge, constraining actions and measuring performance. NEXUS does not reproduce any cited system or claim their reported results. The bibliography below is transcribed from the baseline; detailed literature claims should be checked against the papers before submission.

## System design

The user interface uses Next.js, React, TypeScript and Tailwind with a React Flow dependency graph. A same-origin proxy forwards requests to the Java backend. Spring Security controls access, Spring JDBC persists records, Micrometer exposes metrics and scheduled Java collection drives incident detection. Java agent orchestration coordinates typed findings and optional Spring AI calls. PostgreSQL stores evidence/audit JSONB and incident memory. The observability deployment combines Prometheus, an OpenTelemetry Collector, Jaeger and Loki. The sandbox uses a separate database and internal network.

The full architecture and state diagram appear in `architecture.md`. Security, evidence provenance and interrupted-work behavior are part of the implementation rather than presentation-only features.

## Method

1. Collect application health, latency, resource metrics, log events and trace responses with source IDs and times.
2. Open one active incident per service after two consecutive anomalous scans.
3. Examine log availability/messages and traverse unhealthy dependencies in the declared graph.
4. Retrieve similar incident records using embeddings/cosine search or a labelled lexical fallback.
5. Combine findings into a probable cause with explicit uncertainty and evidence links. Optional LLM interpretations must cite known available evidence.
6. Select a fixed supported plan or require manual investigation. AI does not control execution policy.
7. Reproduce the fault in the behavior replica, clear it and verify three application responses.
8. Require developer approval for the current incident revision within five minutes, checking live state again.
9. Execute the fixed reset, verify health, record outcome and add the incident to memory.

## Data model

Incidents have an identifier, target service, state, timestamps, revision, evidence, diagnosis, validation and outcome. Approvals retain actor and time. Audit events preserve decisions and attempted actions. Historical memory stores a bounded summary and optional model-labelled vector. Samples support telemetry inspection and are retained for seven days. The schema and its constraints are defined by a Flyway migration.

## Experimental design

Evaluate each supported fault on each service, with repeat trials and healthy baselines. Record injection time, detection time, diagnosis availability, whether the root service is correct, validation outcome, approval time, execution duration and verification result. Separate human approval delay from automatic processing time. Compare deterministic mode against enabled AI with and without historical context, keeping fault scenarios and model configuration fixed. Use independent incidents for held-out evaluation to avoid treating memorized duplicates as generalization. See `evaluation.md` for formulas and threats to validity.

## Results status

Automated verification and environment constraints are recorded in `verification.md`. Compilation, unit tests and fixture-based browser tests demonstrate selected implementation properties. They cannot establish deployment health, LLM quality, live vector retrieval, diagnosis accuracy or reduced mean time to recovery. The evaluation table must remain unfilled until trials have actually run. No quantitative performance claim is made here.

## Safety, limitations and ethics

Application logs may contain sensitive information. Deploy only the isolated demo without personal data; review/redact evidence before enabling external model calls. Model outputs are treated as unverified interpretations, even with valid citations. Recovery is allowlisted, validated and approved; a failed or interrupted action is not blindly repeated. Database administrators can alter audit records, so durable storage is not equivalent to an immutable audit ledger. The fixed topology and known injected modes make this a controlled study, not evidence of general production root-cause accuracy.

## Future work

Potential extensions include trace-derived dependency discovery, resource baselines, independently evaluated model ensembles, richer historical ranking, distributed execution leases, cancellation/time-budget control, identity-provider integration, tamper-evident external audit storage and more faithful staging environments. Each new executable action requires a separate risk policy and meaningful validation before it is enabled.

## Bibliography from the supplied presentation

1. Pedroso et al. (2025), “Anomaly detection and root cause analysis in cloud-native environments using large language models and Bayesian networks,” IEEE Access 13, 77550–77564. [IEEE record](https://ieeexplore.ieee.org/document/10979844).
2. Ahmed et al. (2023), “Recommending root-cause and mitigation steps for cloud incidents using large language models,” ICSE, 1737–1749. [IEEE record](https://ieeexplore.ieee.org/document/10172904).
3. Chen et al. (2025), “STRATUS: A multi-agent system for autonomous reliability engineering of modern clouds.” [arXiv](https://arxiv.org/abs/2506.02009).
4. Chen et al. (2024), “Automatic root cause analysis via large language models for cloud incidents,” EuroSys, 674–688. [arXiv](https://arxiv.org/abs/2305.15778).
5. Chen et al. (2025), “AIOpsLab: A holistic framework for evaluating AI agents for enabling autonomous cloud,” MLSys. [Microsoft Research](https://www.microsoft.com/en-us/research/publication/aiopslab-a-holistic-framework-for-evaluating-ai-agents-for-enabling-autonomous-cloud/).
6. Shetty et al. (2022), “AutoTSG: Learning and synthesis for incident troubleshooting,” ESEC/FSE. [arXiv](https://arxiv.org/abs/2205.13457).
7. Lee et al. (2023), “Eadro: An end-to-end troubleshooting framework for microservices on multi-source data,” ICSE, 1750–1762. [arXiv](https://arxiv.org/abs/2302.05092).
8. Zhang et al. (2023), “Robust failure diagnosis of microservice system through multimodal data,” IEEE TSC 16(6), 3851–3864. [arXiv](https://arxiv.org/abs/2302.10512).
9. Gu et al. (2023), “TrinityRCL: Multi-granular and code-level root cause localization using multiple types of telemetry data in microservice systems,” IEEE TSE 49(5), 3071–3088. [IEEE record](https://ieeexplore.ieee.org/document/10034937).
10. Sui et al. (2023), “LogKG: Log failure diagnosis through knowledge graph,” IEEE TSC 16(5), 3493–3507. [IEEE record](https://ieeexplore.ieee.org/abstract/document/10179162).

This report is an implementation draft for team review. Confirm institution formatting, author spelling, references and measured results before academic submission.
