# API reference

All application routes are under `/api`. The console uses the same-origin Next.js proxy. Cookies are HTTP session credentials; no token is stored in browser local storage. Mutations require `X-XSRF-TOKEN`, obtained from GET `/api/csrf`. After login/logout obtain a fresh token. Login uses URL-encoded fields; other mutations use JSON. GET routes require either role except CSRF; business mutations require DEVELOPER. Logout is the standard Spring Security session logout and is available to authenticated users of either role.

| Method | Route | Request / response |
|---|---|---|
| GET | `/csrf` | `{token, headerName}`; available anonymously |
| POST | `/login` | `username`, `password` as form data; 204 success, 401 failure |
| POST | `/logout` | CSRF header; invalidates session |
| GET | `/me` | User, authorities and demo flag |
| GET | `/services` | Fixed service registry, dependencies, latest snapshots |
| GET | `/incidents` | Up to 100 incidents, newest first |
| GET | `/incidents/{uuid}` | Complete incident |
| GET | `/incidents/{uuid}/audit` | Ordered actor/event/detail/time records |
| GET | `/samples` | Up to 300 samples from the last 30 minutes |
| GET | `/memory?q=inventory` | Up to 3 results, retrieval type/model behavior stated in UI |
| POST | `/memory/reindex` | Generate missing or model-mismatched embeddings; requires AI |
| POST | `/scan` | `{}`; collect a real scan, return `{complete:true}` |
| POST | `/demo/fault` | `{service:"inventory", mode:"ERROR"}`; fixed registry only |
| POST | `/incidents/{uuid}/investigate` | `{}`; retry a DETECTED incident's diagnosis |
| POST | `/incidents/{uuid}/validate` | `{}`; requires AWAITING_VALIDATION |
| POST | `/incidents/{uuid}/revalidate` | `{}`; rerun validation from AWAITING_APPROVAL |
| POST | `/incidents/{uuid}/approve` | `{version:4, confirmed:true}`; claims execution and verifies |
| POST | `/incidents/{uuid}/close` | `{reason:"Dependency recovered; probes reviewed."}` |

`/approve` represents a specific human decision. Do not submit it merely because an LLM suggested an action. The live-demo script only does so with an explicit `--approve` flag.

HTTP 400 represents invalid fields, 401 requires sign-in, 403 indicates role or CSRF rejection, 404 indicates an unknown incident, 409 means a state/revision/validation conflict and 503 means a failed dependency. Inspect the audit and live state after an interrupted or timed-out action before deciding what to do next. There is no blind automatic retry of recovery.

Supported services: `orders`, `payments`, `inventory`. Supported demo modes: `NONE`, `ERROR`, `LATENCY`, `DB_POOL`. Supported executable recovery: `RESET_FAULT`. Requests cannot supply arbitrary network targets or commands.

Framework validation/malformed-body errors can use Spring's standard error payload. Business errors use `{error:"..."}`. Credentials, session cookies, API keys and demo control tokens must not be included in screenshots or submitted documentation.
