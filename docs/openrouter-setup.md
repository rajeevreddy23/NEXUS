# OpenRouter setup for NEXUS

Save your OpenRouter key only in the ignored local `.env`. The variable remains named `OPENAI_API_KEY` because Spring AI's OpenAI-compatible client uses it; the configured host routes requests to OpenRouter.

```dotenv
AI_ENABLED=true
AI_BASE_URL=https://openrouter.ai/api
CHAT_MODEL=nvidia/nemotron-3-ultra-550b-a55b:free
EMBEDDING_MODEL=nvidia/nemotron-3.5-embed-1b:free
```

Spring AI appends `/v1/chat/completions` and `/v1/embeddings` to this base. Using `https://openrouter.ai/api/v1` as the Spring base would duplicate `/v1`.

NEXUS explicitly requests 1536 embedding dimensions. The free Nemotron embedding endpoint accepted that parameter and returned a 1536-element numeric vector in the direct API smoke check on 27 October 2026. Its default is 2048, so a model-only environment change without the dimension option would fail NEXUS's schema guard. The database schema remains unchanged, preserving existing incident text and vectors. Model identity still filters retrieval; use reindexing to replace old-model embeddings.

The subsequent real Java/Spring AI integration check passed too: one cited JSON response plus a 1536-element finite embedding, with no database or UI fixture substituting for the provider. AI is now enabled in this workspace's private `.env`; fresh example environments remain disabled until a key is supplied.

The free Nemotron chat model supports disabling its extra reasoning through `reasoning.enabled=false`. NEXUS sends that extension for NVIDIA `:free` chat IDs only, to leave the bounded response budget for cited JSON. Spring AI 1.1 drops this option during model-option merging, so a narrow HTTP interceptor adds it after serialization; the local protocol test checks the final HTTP request. Other model IDs do not receive that extension. Findings remain unverified interpretations and never become executable commands.

## Check without Docker

```powershell
pwsh -File scripts/check-ai.ps1
```

This explicit test sends two small synthetic requests through the actual Java/Spring AI client. It does not connect to the application database, ingest personal logs or execute recovery. Credentials are read from `.env`, are not printed, and the script restores its inherited environment after completion. Ordinary Maven checks skip this live test unless `NEXUS_LIVE_AI_TEST=true` is deliberately set.

## Start the complete application

Once Docker Desktop's Linux engine is running:

```powershell
docker compose config --quiet
docker compose up --build -d
```

If the stack was already running, recreate the backend after changing `.env`. Rebuilding is necessary when Java source has changed: `docker compose up --build -d backend`.

Successful provider requests establish connection and response compatibility. They do not establish live PGvector retrieval, collector delivery, application authentication or recovery in the Compose deployment; those still require the full stack. Quotas or unavailable providers result in a labelled deterministic/text-search fallback, not fabricated AI success.

References: [OpenRouter embeddings API](https://openrouter.ai/docs/api/reference/embeddings), [Nemotron chat](https://openrouter.ai/nvidia/nemotron-3-ultra-550b-a55b:free), [Nemotron embeddings](https://openrouter.ai/nvidia/nemotron-3.5-embed-1b:free).



## Investigation citations

The full incident prompt uses per-request aliases for available evidence, mapped back to immutable database evidence IDs after strict validation. The first cloud model investigations returned parseable JSON but referenced IDs outside the permitted evidence set; they were correctly labelled fallback rather than counted as successful AI. The prompt/validator contract now excludes unavailable sources and states the permitted aliases explicitly. Safe diagnostics expose exception categories and known validation reasons, never provider response bodies, headers or keys. Successful vector retrieval alone is not proof of accepted model reasoning.
