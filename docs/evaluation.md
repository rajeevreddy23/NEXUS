# Reproducible evaluation protocol

## Questions

1. How quickly does NEXUS detect each injected failure?
2. How often does its probable root service match the injection target?
3. Do supported recovery actions pass validation and restore application probes?
4. Does retrieved memory provide relevant evidence, and does optional AI improve diagnosis quality over deterministic analysis?
5. Are unsafe or stale approval requests rejected?

## Trial matrix

Run the 3 fault types (ERROR, LATENCY, DB_POOL) against all 3 services, with at least 10 repeats per condition. Include healthy control windows and collector-unavailable cases. Evaluate cold memory, accumulated memory and AI-enabled conditions separately. Log exact Java, dependency/image versions, thresholds, provider/model names, machine resources, database size and date for each experiment.

The injected mode is an oracle for this demo. Report that the deterministic diagnostic algorithm observes the mode from the demo state endpoint; this makes the study easier than unknown production diagnosis. For a more demanding study, hide the mode from the diagnosis stage and require independent ground-truth annotation. Do not present oracle-assisted results as general root-cause intelligence.

## Procedure

1. Start the Compose deployment and verify Prometheus targets, Loki readiness and Jaeger trace delivery.
2. Ensure all work probes pass, all previous faults are cleared, and prior open incident records are closed with notes.
3. Record a healthy observation window of at least five minutes. Measure false incident openings.
4. Record fault injection at `t_inject`. Capture the detector's `openedAt` as `t_detect`. Record diagnosis completion from the audit, sandbox results, approval time and recovery completion separately.
5. Ask the developer to inspect evidence and approve only the intended fixed action. Record invalid/stale approval cases separately from normal recovery.
6. Verify both the root service and the complete dependency chain after recovery. Close propagated incident records with review notes.
7. Save the incident, audit and actual measurements. Repeat in a fresh or documented memory condition.

`node scripts/live-demo.mjs --approve` exercises the ERROR/inventory condition and writes a JSON measurement artifact from live API timestamps. It covers one scenario and cannot substitute for the full matrix. It does not produce fabricated trial results. Its detection measurement includes the time needed for HTTP fault application; instrument the exact fault-change event timestamp for more precise experiments.

## Metrics

| Metric | Definition |
|---|---|
| Detection delay | `t_detect - t_inject` |
| Diagnosis delay | `t_diagnosis_complete - t_detect` |
| Top-1 root-service accuracy | Correct predicted root service / evaluated incidents |
| Sandbox pass rate | Successful reproduction-and-reset tests / validation attempts |
| Verified recovery rate | Actions with three passing post-action probes / executed actions |
| Automatic recovery duration | Verification completion minus execution start |
| Human decision delay | Approval time minus validation completion |
| Total restoration time | Verified restoration minus injection time |
| False incident rate | Incidents opened in healthy windows / healthy observation hours |
| Citation validity | AI findings using known available evidence IDs / AI findings |
| Citation relevance | Independently judged support for each claim; distinct from ID validity |
| Memory relevance | Expert-labelled useful retrieved records / retrieved records |

Report counts, medians and p95 latencies with uncertainty. Failure and timeout trials belong in denominators; do not exclude them silently. Never infer reduced MTTR without an explicitly described comparison condition.

## Measured cloud pilot: 27 October 2026

[Run 40561946483](https://github.com/rajeevreddy23/NEXUS/actions/runs/40561946483), code `479abf2bc05950964602c19b2f10d56a352e8648`, executed on a standard Ubuntu 24.04.5 runner (2 CPUs, about 7.75 GiB RAM, Docker 28.0.4). The full deterministic acceptance and authenticated browser-shell test passed; the later AI step failed, so the **overall workflow conclusion is failure**, not a green deployment badge.

Actual machine-readable records are in `evidence/cloud-40561946483/acceptance.json` and `java.json`. Each row below is **one inventory trial**, with explicit scripted developer approval for RESET_FAULT. Detection includes requested scans and can overlap scheduled collection. These are controlled oracle-assisted smoke measurements, not automatic-only detection benchmarks or estimates of production accuracy.

| Scenario | Trials | Injection to incident | Whole trial | Recovery plus post-checks | Correct root / verified reset |
|---|---:|---:|---:|---:|---|
| ERROR | 1 | 1,524 ms | 5,357 ms | 1,565 ms | Yes / Yes |
| LATENCY | 1 | 10,334 ms | 21,327 ms | 1,241 ms | Yes / Yes |
| DB_POOL | 1 | 5,000 ms | 8,163 ms | 1,077 ms | Yes / Yes |

The artifact's `recoveryMs` includes the approval API call and subsequent service/memory/audit checks before the row is written; it is not a pure action-runtime measurement. Whole trial excludes healthy settling/cleanup after that row. No human decision-time comparison was made. No median/p95 estimate, MTTR improvement, generalized diagnosis accuracy or AI benefit follows from these three observations.

The same run passed anonymous/observer/CSRF restrictions, unvalidated/unconfirmed/stale-revision/replayed approvals, expired sandbox validation, changed live fault state, real three-connection pool exhaustion, memory/audit checks, four Prometheus scrape targets, nonempty request/JVM series, Loki log streams and Jaeger spans. The changed-state check retained LATENCY rather than silently resetting it. Expiry used a deliberately aged validation timestamp in the disposable database.

## Enabled AI/RAG smoke result

The corrected [run 40564112317](https://github.com/rajeevreddy23/NEXUS/actions/runs/40564112317) passed overall. Its targeted scope repeated one seed ERROR recovery and real collectors, verified a populated browser graph, and then ran one orders ERROR incident with AI enabled. Three model roles returned accepted citations, historical context used PGvector, the approved sandbox-tested recovery resolved, and the resulting memory was retrieved using vector search. Actual outputs are in `evidence/cloud-40564112317/ai-rag.json` and `ai-attempt.json`. The six affected Java tests, production frontend build and five fixtures also passed.

This is identity/transport/integration validation, not an expert score for the model's reasoning. The explanations were similar across roles and discussed earlier downstream faults as possible context for the present orders fault. That temporal/causal ambiguity remains a limitation despite valid citations. No model-quality or AI-versus-deterministic benefit is inferred from this single sample. Free provider calls were deliberately bounded; unavailable and rejected attempts were retained as failures.

## Larger study worksheet (not yet measured)

| Scenario | Trials | Detection median | Root accuracy | Validation pass | Recovery success |
|---|---:|---:|---:|---:|---:|
| ERROR | Not run | — | — | — | — |
| LATENCY | Not run | — | — | — | — |
| DB_POOL | Not run | — | — | — | — |

## Threats to validity

Known injected modes, fixed topology, one machine, limited faults and deterministic recovery all restrict external validity. Agent and model versions can change. Historical records can leak evaluation cases into the prompt. Human approval delay can dominate end-to-end time. Collector failures and stale snapshots can affect evidence coverage. The simplified sandbox excludes downstream dependencies. Heuristic confidence is not a calibrated probability. UI fixture tests do not supply empirical system-performance results.

