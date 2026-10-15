// Request validation for POST /v1/chat. Pure functions so they can be unit-tested.

export const LIMITS = Object.freeze({
  maxMessages: 12, // the app sends only the recent part of a conversation
  maxTextChars: 2000,
  maxContextChars: 4000,
  maxImageBase64Chars: 2_000_000, // about 1.5 MB decoded; the app downsizes photos first
});

const IMAGE_TYPES = new Set(["image/jpeg", "image/png", "image/webp"]);

export class ValidationError extends Error {}

/**
 * Checks the app's request and turns it into Claude API messages.
 * Body shape:
 *   { assistant: "garden" | "aquarium",
 *     messages: [{ role: "user" | "assistant", text: string }],   // oldest first, last one from the user
 *     context?: string,                                            // plant / tank details the user chose to attach
 *     image?: { mediaType: "image/jpeg", data: "<base64>" } }      // attached to the last user message
 */
export function toClaudeRequest(body) {
  if (!body || typeof body !== "object") throw new ValidationError("Body must be a JSON object.");
  const { assistant, messages, context, image } = body;

  if (assistant !== "garden" && assistant !== "aquarium") throw new ValidationError("assistant must be 'garden' or 'aquarium'.");
  if (!Array.isArray(messages) || messages.length === 0) throw new ValidationError("messages must be a non-empty array.");

  const recent = messages.slice(-LIMITS.maxMessages);
  for (const m of recent) {
    if (!m || (m.role !== "user" && m.role !== "assistant")) throw new ValidationError("Each message needs role 'user' or 'assistant'.");
    if (typeof m.text !== "string" || m.text.trim() === "") throw new ValidationError("Each message needs non-empty text.");
    if (m.text.length > LIMITS.maxTextChars) throw new ValidationError(`Messages are limited to ${LIMITS.maxTextChars} characters.`);
  }
  // The API requires the conversation to start with the user; trimming may have cut that.
  while (recent.length > 0 && recent[0].role !== "user") recent.shift();
  if (recent.length === 0 || recent.at(-1).role !== "user") throw new ValidationError("The last message must be from the user.");

  if (context != null && (typeof context !== "string" || context.length > LIMITS.maxContextChars)) {
    throw new ValidationError(`context must be text of at most ${LIMITS.maxContextChars} characters.`);
  }
  if (image != null) {
    if (!IMAGE_TYPES.has(image.mediaType)) throw new ValidationError("image.mediaType must be image/jpeg, image/png or image/webp.");
    if (typeof image.data !== "string" || image.data.length === 0 || image.data.length > LIMITS.maxImageBase64Chars) {
      throw new ValidationError("image.data must be base64 of a photo under about 1.5 MB.");
    }
  }

  const claudeMessages = recent.map((m, i) => {
    const isLast = i === recent.length - 1;
    if (!isLast) return { role: m.role, content: m.text };
    const content = [];
    if (image) content.push({ type: "image", source: { type: "base64", media_type: image.mediaType, data: image.data } });
    if (context && context.trim()) content.push({ type: "text", text: `Details from my AquaGrow app:\n${context.trim()}` });
    content.push({ type: "text", text: m.text });
    return { role: "user", content };
  });

  return { assistant, messages: claudeMessages };
}
