import assert from "node:assert/strict";
import { test } from "node:test";
import { createApp } from "../src/server.js";
import { createRateLimiter } from "../src/rateLimit.js";

/** Fake Claude client: records the request and returns a canned response. */
function fakeClient(response) {
  const calls = [];
  return {
    calls,
    beta: { messages: { create: async (params) => { calls.push(params); return response; } } },
  };
}

async function withServer(app, fn) {
  const server = app.listen(0);
  await new Promise((r) => server.once("listening", r));
  try {
    return await fn(`http://127.0.0.1:${server.address().port}`);
  } finally {
    server.close();
  }
}

const post = (base, body) =>
  fetch(`${base}/v1/chat`, { method: "POST", headers: { "content-type": "application/json" }, body: JSON.stringify(body) });

test("returns the text reply and sends the garden system prompt", async () => {
  const client = fakeClient({ stop_reason: "end_turn", content: [{ type: "text", text: "Check the soil first." }] });
  await withServer(createApp({ client }), async (base) => {
    const res = await post(base, { assistant: "garden", messages: [{ role: "user", text: "How often to water tomatoes?" }] });
    assert.equal(res.status, 200);
    assert.deepEqual(await res.json(), { reply: "Check the soil first.", refused: false, truncated: false });
  });
  assert.match(client.calls[0].system, /Never give one fixed watering interval/);
  assert.equal(client.calls[0].fallbacks, "default");
});

test("reports a refusal without content", async () => {
  const client = fakeClient({ stop_reason: "refusal", content: [] });
  await withServer(createApp({ client }), async (base) => {
    const res = await post(base, { assistant: "aquarium", messages: [{ role: "user", text: "x" }] });
    assert.deepEqual(await res.json(), { reply: null, refused: true });
  });
});

test("400 for invalid body, 429 with Retry-After when over the limit", async () => {
  const client = fakeClient({ stop_reason: "end_turn", content: [{ type: "text", text: "ok" }] });
  const limiter = createRateLimiter({ limit: 1, windowMs: 60_000 });
  await withServer(createApp({ client, limiter }), async (base) => {
    assert.equal((await post(base, { assistant: "nope" })).status, 400);
    const res = await post(base, { assistant: "garden", messages: [{ role: "user", text: "hi" }] });
    assert.equal(res.status, 429);
    assert.ok(Number(res.headers.get("retry-after")) > 0);
  });
});
