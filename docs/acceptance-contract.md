# Acceptance contract

“Complete” means the defined application and reliability workflows are implemented and pass reproducible checks. It cannot mean every possible input, infrastructure fault, security attack or provider response is guaranteed to work. Unsupported causes must remain visible and require manual investigation.

| Area | Required behavior | Automated check |
|---|---|---|
| Accounts | Real database accounts; hashed passwords; separate application/control sessions | PostgreSQL integration, live API, browser |
| Authorization | Members see their projects; only owners/admins manage membership; control observers cannot recover | Integration and live negative requests |
| Request protection | CSRF required; invalid fields rejected; no arbitrary target URLs/commands | Java security and validation tests |
| Business work | Create/edit/archive projects; add members; create/assign/update/complete tasks; inspect history | Live API and real browser-created data |
| Concurrency | Stale project/task revisions fail; archival cannot race a task write | PostgreSQL checks and conditional SQL |
| Persistence | Accounts and tasks survive service restart and database reconnect | Disposable Compose acceptance |
| Monitoring | Actual readiness, CPU/memory/connection metrics, logs and traces; missing sources labelled | Telemetry tests and nonempty collector responses |
| Detection | Repeated anomalous observations open one active incident per service | Rule tests and live scans |
| Diagnosis | Evidence citations retained; missing evidence does not authorize repair | Grounding tests and unavailable-state checks |
| AI and memory | Three read-only roles; real historical PGvector context and newly stored outcome retrieval | Explicit free-provider scenario; deterministic fallback tests |
| Isolated validation | Same application build and separate DB; cleanup; passing probes | Runbook tests and live acceptance |
| Approval | Unvalidated, observer, unconfirmed, stale, expired, replayed or changed-state requests rejected | Unit/security checks and live approval cases |
| Recovery | Only supported fixed action; three post-action probes; failed result stays unresolved | Unit tests and real approved maintenance recovery |
| Unsupported outage | An actual stopped database yields unavailable service and manual investigation | Live database stop/reconnect |
| Crash ambiguity | Restart during execution marks result unknown and never replays; fresh validation required after interrupted validation | Startup reconciliation and disposable interrupted-action fixture |
| Backup | Restore a business backup into a separate database and verify a record | Disposable restore check |
| Original microservices | ERROR, LATENCY, real held-connection exhaustion, dependency graph and approved recovery preserved | Full laboratory regression workflow |
| Delivery | Setup preserves secrets, builds compile, tests have truthful skip reporting, instructions distinguish lab/application | Setup helpers, build and cloud evidence ledger |

The live application acceptance uses explicit scans with scheduled collection disabled to keep test fixtures deterministic. Normal application deployment enables scheduled collection by default. The API checks and browser checks operate on actual services and PostgreSQL; the five console fixture tests are separately identified.

Continuous public hosting is a deployment requirement, not satisfied by a successful ephemeral workflow. High availability, arbitrary infrastructure repair, enterprise SSO, public account registration/email recovery, multi-controller coordination, broad performance claims and a statistical accuracy study are outside this implemented scope. The real Tasks application uses administrator-provisioned accounts and reversible project archival; it does not expose destructive task deletion. See the verification ledger for measured results and remaining-work document for operational limits.
