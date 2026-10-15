# AquaGrow assistant backend

A small Node.js server that sits between the AquaGrow app and the Claude API.

```
Phone (AquaGrow) ──HTTPS──▶ this server ──▶ Claude API
                     no key             ANTHROPIC_API_KEY lives here only
```

- The app never contains an API key and never calls an AI provider directly.
- The server adds the safety rules (no fish-illness diagnosis, no fixed watering intervals, fertilizer label limits) as the system prompt.
- It checks every request, limits each IP to 20 questions per 10 minutes, and turns upstream errors into simple codes the app understands.
- It stores nothing: no database and no logging of questions.

## Run locally

Needs Node.js 20 or newer.

```bash
cd backend
npm install
export ANTHROPIC_API_KEY=sk-ant-...      # Windows PowerShell: $env:ANTHROPIC_API_KEY="sk-ant-..."
npm start                               # listens on http://localhost:8787
npm test                                # unit tests, no key needed
```

Point a **debug** build of the app at it by adding this line to `local.properties` in the project root:

```
assistant.baseUrl=http://10.0.2.2:8787
```

`10.0.2.2` is how the Android emulator reaches your computer. Debug builds allow plain HTTP only to `10.0.2.2` and `localhost`; release builds accept HTTPS only.

## Deploy

Any Node host works (Render, Railway, Fly.io, a small VM, Cloud Run).

1. Deploy the `backend/` folder. The start command is `npm start`.
2. Set `ANTHROPIC_API_KEY` in the host's secret or environment settings. Never commit it.
3. Put the HTTPS URL in `assistant.properties` in the project root (`baseUrl=https://...`) and build the app.

Optional settings (see `.env.example`):

| Variable | Default | Meaning |
|---|---|---|
| `PORT` | `8787` | Port to listen on |
| `CLAUDE_MODEL` | `claude-opus-5-5` | Model used for answers |
| `CLAUDE_EFFORT` | `low` | `low` / `medium` / `high`; higher is slower and costs more |
| `RATE_LIMIT_PER_10_MIN` | `20` | Questions per IP per 10 minutes |

The rate limiter is in memory, so it only works across one server instance. If you run several, use your platform's rate limiting.

Anyone who knows your URL can call it, so keep the rate limit on and set a spending limit in the Anthropic Console.

## API

`POST /v1/chat`

```json
{
  "assistant": "garden",
  "messages": [{ "role": "user", "text": "Why are my tomato leaves yellow?" }],
  "context": "Plant: Tomato, Cherry\nPlanted: 1 Sep 2026",
  "image": { "mediaType": "image/jpeg", "data": "<base64>" }
}
```

- `assistant` is either `"garden"` or `"aquarium"`.
- `context` and `image` are optional.

Responses:

| Status | Body | Meaning |
|---|---|---|
| 200 | `{ "reply": "...", "refused": false, "truncated": false }` | Answer |
| 200 | `{ "reply": null, "refused": true }` | The model declined the question |
| 400 | `{ "error": "bad_request", "message": "..." }` | Invalid request |
| 429 | `{ "error": "rate_limited", "retryAfterSeconds": 30 }` | Too many questions; also sent as a `Retry-After` header |
| 500 / 503 | `{ "error": "server_misconfigured" \| "upstream_unavailable" \| "internal" }` | Server or Claude API problem |

`GET /health` returns `{ "ok": true }`.

## Refusal fallback

Requests use the Claude API's server-side fallback (`fallbacks: "default"`, beta `server-side-fallback-2026-07-01`). If a safety classifier declines a harmless question, Anthropic retries it on a recommended fallback model. If every model declines, the app shows a polite "can't help with that" message.

To turn this off, delete the `betas` and `fallbacks` lines in `src/server.js`.
