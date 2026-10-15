// Small in-memory fixed-window rate limiter, per client IP. Good enough for one server
// instance; use your platform's rate limiting (or Redis) if you run several.

export function createRateLimiter({ limit, windowMs, now = () => Date.now() }) {
  const hits = new Map(); // key -> { count, resetAt }

  return function check(key) {
    const t = now();
    let entry = hits.get(key);
    if (!entry || t >= entry.resetAt) {
      entry = { count: 0, resetAt: t + windowMs };
      hits.set(key, entry);
    }
    entry.count++;
    if (hits.size > 10_000) {
      for (const [k, v] of hits) if (t >= v.resetAt) hits.delete(k);
    }
    if (entry.count > limit) {
      return { allowed: false, retryAfterSeconds: Math.max(1, Math.ceil((entry.resetAt - t) / 1000)) };
    }
    return { allowed: true, retryAfterSeconds: 0 };
  };
}
