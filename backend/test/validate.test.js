import assert from "node:assert/strict";
import { test } from "node:test";
import { createRateLimiter } from "../src/rateLimit.js";
import { LIMITS, toClaudeRequest, ValidationError } from "../src/validate.js";

const ask = (text) => ({ role: "user", text });

test("builds Claude messages with context and image on the last user turn", () => {
  const r = toClaudeRequest({
    assistant: "aquarium",
    messages: [ask("Hi"), { role: "assistant", text: "Hello" }, ask("Why is my water cloudy?")],
    context: "Tank: 72 L planted",
    image: { mediaType: "image/jpeg", data: "abc" },
  });
  assert.equal(r.assistant, "aquarium");
  assert.equal(r.messages.length, 3);
  assert.equal(r.messages[0].content, "Hi");
  const last = r.messages[2].content;
  assert.equal(last[0].type, "image");
  assert.match(last[1].text, /72 L planted/);
  assert.equal(last[2].text, "Why is my water cloudy?");
});

test("keeps only recent messages and starts with the user", () => {
  const many = [];
  for (let i = 0; i < 20; i++) many.push(i % 2 === 0 ? ask(`q${i}`) : { role: "assistant", text: `a${i}` });
  many.push(ask("last"));
  const r = toClaudeRequest({ assistant: "garden", messages: many });
  assert.ok(r.messages.length <= LIMITS.maxMessages);
  assert.equal(r.messages[0].role, "user");
  assert.equal(r.messages.at(-1).content.at(-1).text, "last");
});

test("rejects bad input", () => {
  const bad = [
    null,
    { assistant: "chat", messages: [ask("x")] },
    { assistant: "garden", messages: [] },
    { assistant: "garden", messages: [{ role: "assistant", text: "x" }] },
    { assistant: "garden", messages: [ask("x".repeat(LIMITS.maxTextChars + 1))] },
    { assistant: "garden", messages: [ask("x")], image: { mediaType: "image/gif", data: "a" } },
    { assistant: "garden", messages: [ask("x")], context: 5 },
  ];
  for (const body of bad) assert.throws(() => toClaudeRequest(body), ValidationError);
});

test("rate limiter blocks after the limit and resets after the window", () => {
  let t = 0;
  const check = createRateLimiter({ limit: 2, windowMs: 1000, now: () => t });
  assert.equal(check("a").allowed, true);
  assert.equal(check("a").allowed, true);
  const blocked = check("a");
  assert.equal(blocked.allowed, false);
  assert.equal(blocked.retryAfterSeconds, 1);
  assert.equal(check("b").allowed, true);
  t = 1000;
  assert.equal(check("a").allowed, true);
});
