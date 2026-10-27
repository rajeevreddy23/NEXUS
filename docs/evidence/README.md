# Runtime evidence provenance

These files contain measured synthetic-demo results, not fixture telemetry. No environment files, cookies, authentication traces, provider keys or raw provider responses are included.

- `cloud-40561946483/acceptance.json`: full deterministic live API/telemetry/safety acceptance passed on code `479abf2bc05950964602c19b2f10d56a352e8648`.
- `cloud-40561946483/java.json`: all 30 non-provider Java tests passed, including Docker PGvector. The opt-in provider unit smoke was skipped.
- The original authenticated screenshot was captured before asynchronous refresh and was not accepted as populated-graph proof. It is not included as final demo evidence.
- `cloud-40563263838/ai-attempt.json`: actual vector retrieval succeeded, but all three model outputs failed citation validation. The overall workflow failed. This is retained as failed evidence, not a successful AI result.
- `cloud-40563263838/acceptance.json`: a fresh ERROR seed trial and telemetry checks for the targeted AI investigation. It does not repeat the complete three-fault matrix.

GitHub Actions runner data is ephemeral. Source-run links and code identifiers are recorded in the verification ledger. Artifact retention is three days; these reviewed evidence files are kept in the private repository for reproducibility.

- `cloud-40564112317/acceptance.json`: final targeted seed recovery and collector checks passed.
- `cloud-40564112317/ai-rag.json`: final enabled AI/PGvector/approved-recovery integration passed.
- `cloud-40564112317/ai-attempt.json`: actual accepted model interpretations with immutable citation IDs; semantic correctness remains unverified.
- `cloud-40564112317/live-console.png`: inspected live authenticated console after three operational nodes and two dependency edges loaded.
