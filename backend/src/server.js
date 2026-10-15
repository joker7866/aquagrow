// AquaGrow assistant proxy. The app calls this server; only this server calls the Claude API.
// The API key comes from the ANTHROPIC_API_KEY environment variable and never leaves the server.

import Anthropic from "@anthropic-ai/sdk";
import express from "express";
import { pathToFileURL } from "node:url";
import { SYSTEM_PROMPTS } from "./prompts.js";
import { createRateLimiter } from "./rateLimit.js";
import { toClaudeRequest, ValidationError } from "./validate.js";

const PORT = Number(process.env.PORT ?? 8787);
const MODEL = process.env.CLAUDE_MODEL ?? "claude-opus-5-5";
// Short chat answers do well at low effort; raise it if answers feel shallow.
const EFFORT = process.env.CLAUDE_EFFORT ?? "low";
const RATE_LIMIT = Number(process.env.RATE_LIMIT_PER_10_MIN ?? 20);

export function createApp({ client = new Anthropic(), limiter = createRateLimiter({ limit: RATE_LIMIT, windowMs: 10 * 60_000 }) } = {}) {
  const app = express();
  app.disable("x-powered-by");
  app.set("trust proxy", 1); // so req.ip is the phone's address behind one proxy / load balancer
  app.use(express.json({ limit: "3mb" }));

  app.get("/health", (_req, res) => res.json({ ok: true }));

  app.post("/v1/chat", async (req, res) => {
    const rl = limiter(req.ip ?? "unknown");
    if (!rl.allowed) {
      res.set("Retry-After", String(rl.retryAfterSeconds));
      return res.status(429).json({ error: "rate_limited", retryAfterSeconds: rl.retryAfterSeconds });
    }

    let request;
    try {
      request = toClaudeRequest(req.body);
    } catch (e) {
      if (e instanceof ValidationError) return res.status(400).json({ error: "bad_request", message: e.message });
      throw e;
    }

    try {
      const response = await client.beta.messages.create({
        model: MODEL,
        max_tokens: 16000,
        system: SYSTEM_PROMPTS[request.assistant],
        messages: request.messages,
        output_config: { effort: EFFORT },
        // If a safety classifier declines, Anthropic re-runs the request on its recommended
        // fallback model instead of returning the refusal.
        betas: ["server-side-fallback-2026-07-01"],
        fallbacks: "default",
      });

      if (response.stop_reason === "refusal") {
        return res.json({ reply: null, refused: true });
      }
      const reply = response.content
        .filter((b) => b.type === "text")
        .map((b) => b.text)
        .join("\n")
        .trim();
      return res.json({ reply, refused: false, truncated: response.stop_reason === "max_tokens" });
    } catch (e) {
      return sendUpstreamError(res, e);
    }
  });

  return app;
}

function sendUpstreamError(res, e) {
  if (e instanceof Anthropic.RateLimitError) {
    const retry = Number(e.headers?.get?.("retry-after")) || 30;
    res.set("Retry-After", String(retry));
    return res.status(429).json({ error: "rate_limited", retryAfterSeconds: retry });
  }
  if (e instanceof Anthropic.AuthenticationError || e instanceof Anthropic.PermissionDeniedError) {
    console.error("Claude API key is missing or invalid:", e.message);
    return res.status(500).json({ error: "server_misconfigured" });
  }
  if (e instanceof Anthropic.BadRequestError) {
    console.error("Claude API rejected the request:", e.message);
    return res.status(400).json({ error: "bad_request", message: "The question could not be processed." });
  }
  if (e instanceof Anthropic.APIConnectionError || e instanceof Anthropic.APIError) {
    console.error("Claude API unavailable:", e.message);
    return res.status(503).json({ error: "upstream_unavailable" });
  }
  console.error("Unexpected error:", e);
  return res.status(500).json({ error: "internal" });
}

// Start only when run directly (tests import createApp).
if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  if (!process.env.ANTHROPIC_API_KEY && !process.env.ANTHROPIC_AUTH_TOKEN) {
    console.warn("ANTHROPIC_API_KEY is not set; requests will fail until it is.");
  }
  createApp().listen(PORT, () => console.log(`AquaGrow assistant backend on :${PORT} using ${MODEL}`));
}
