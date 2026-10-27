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

## Results worksheet (unmeasured)

| Scenario | Trials | Detection median | Root accuracy | Validation pass | Recovery success |
|---|---:|---:|---:|---:|---:|
| ERROR | Not run | — | — | — | — |
| LATENCY | Not run | — | — | — | — |
| DB_POOL | Not run | — | — | — | — |

## Threats to validity

Known injected modes, fixed topology, one machine, limited faults and deterministic recovery all restrict external validity. Agent and model versions can change. Historical records can leak evaluation cases into the prompt. Human approval delay can dominate end-to-end time. Collector failures and stale snapshots can affect evidence coverage. The simplified sandbox excludes downstream dependencies. Heuristic confidence is not a calibrated probability. UI fixture tests do not supply empirical system-performance results.
