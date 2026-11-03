# API reference

## Real task application

These routes belong to the separate Java Tasks service (`/api` there), exposed to the browser through `/work-api`. They are not NEXUS control-room routes. Form login, logout and CSRF work as below, using the separate `WORKSESSION` session and `WORK-XSRF-TOKEN` cookie. Read the CSRF token from `/work-api/csrf`, send `X-XSRF-TOKEN` on mutations, and refresh it after login/logout.

| Method | Tasks route after `/work-api` | Permission / contract |
|---|---|---|
| GET | `/csrf` | Anonymous; `{token}` |
| POST | `/login`, `/logout` | Form login / CSRF-protected session logout |
| GET | `/me` | Signed-in database account `{id,username,role}` |
| GET / POST | `/projects` | List visible projects / create `{name,description}` |
| POST | `/projects/{id}/update` | Owner/admin; `{name,description,archived,version}` |
| GET / POST | `/projects/{id}/members` | Read by member; owner/admin adds existing `{username}` |
| GET | `/projects/{id}/tasks?limit=50&offset=0` | Member/admin; `{items,total,limit,offset}`, limit 1–200 |
| POST | `/projects/{id}/tasks` | Member/admin; task payload below |
| POST | `/tasks/{id}/update` | Member/admin; task payload and current revision |
| GET | `/projects/{id}/audit` | Member/admin; newest 200 project/task events |
| POST | `/password` | Account owner; `{currentPassword,newPassword}` |
| GET / POST | `/admin/users` | ADMIN; list without hashes / provision `{username,password,role}` |
| GET | `/admin/operations`, `/admin/audit` | ADMIN; operational state / latest 200 business events |
| POST | `/admin/maintenance` | ADMIN; `{enabled,reason}` |

Task payload: `{title,description,status,priority,assigneeId,dueDate,version}`. Status is TODO/IN_PROGRESS/DONE; priority LOW/MEDIUM/HIGH. Assignment is optional but must name an active project member. Due date is optional ISO date. Passwords contain 12–72 characters; usernames use 3–80 letters/digits/underscore/dot/hyphen. Optimistic revisions reject conflicting writes with 409. Archived projects reject task writes. Project deletion is represented by reversible archival; audit records are retained. Task editing and completion are supported; destructive task deletion is not exposed.

Nonmembers receive 404 for project-scoped data; invalid fields/body/identifier return 400. Missing session is 401, role/CSRF rejection 403, duplicate username/conflicting revision 409, maintenance/database unavailability 503. Mutations are transactional with audit writes. Do not retry a timed-out create blindly: reload and check whether it was committed.

Internal M2M routes `/ops/state`, `/ops/events`, `/ops/probe`, and POST `/ops/maintenance` require `X-Nexus-Token`. They are never forwarded by the frontend. The only operational mutation accepts `{enabled,expectedVersion}`; approval supplies a conditional version. `/actuator/metrics/**` uses the same token. Prometheus and health are reachable only on internal service networks. The deployment fixes the target URL through configuration; clients/models cannot supply arbitrary targets or commands.

In application mode, control-room `/me` reports `applicationMode:true,demoEnabled:false`; the allowlisted target is `tasks`, with `RESUME_TRAFFIC` the only executable plan. Unsupported problems use `MANUAL_INVESTIGATION`.

## NEXUS control room

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
