package com.aquagrow.data.assistant

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Wire format of the AquaGrow assistant backend (see /backend/README.md). The app talks only to
// that backend, never to an AI provider, and holds no API key.

@Serializable
data class ChatMessageDto(val role: String, val text: String)

@Serializable
data class ImageDto(val mediaType: String, val data: String)

@Serializable
data class ChatRequestDto(
    val assistant: String,
    val messages: List<ChatMessageDto>,
    val context: String? = null,
    val image: ImageDto? = null,
)

@Serializable
data class ChatResponseDto(val reply: String? = null, val refused: Boolean = false, val truncated: Boolean = false)

/** Non-2xx answer from the backend. */
class AssistantHttpException(val status: Int, val retryAfterSeconds: Int?) : IOException("HTTP $status")

interface AssistantApi {
    /** False when no backend URL was set at build time (assistant.baseUrl). */
    val isConfigured: Boolean

    /** @throws AssistantHttpException for HTTP errors, IOException for network problems. */
    suspend fun chat(request: ChatRequestDto): ChatResponseDto
}

class HttpAssistantApi(baseUrl: String) : AssistantApi {
    private val endpoint = baseUrl.trim().trimEnd('/').takeIf { it.startsWith("https://") || it.startsWith("http://") }?.let { "$it/v1/chat" }
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    override val isConfigured: Boolean get() = endpoint != null

    override suspend fun chat(request: ChatRequestDto): ChatResponseDto = withContext(Dispatchers.IO) {
        val url = endpoint ?: throw IOException("Assistant backend URL is not set")
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15_000
            conn.readTimeout = 90_000 // answers can take a while to generate
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Accept", "application/json")
            conn.outputStream.use { it.write(json.encodeToString(ChatRequestDto.serializer(), request).toByteArray()) }

            val status = conn.responseCode
            if (status !in 200..299) {
                throw AssistantHttpException(status, conn.getHeaderField("Retry-After")?.trim()?.toIntOrNull())
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            try {
                json.decodeFromString(ChatResponseDto.serializer(), body)
            } catch (e: IllegalArgumentException) {
                throw IOException("Unexpected response from the assistant server", e)
            }
        } finally {
            conn.disconnect()
        }
    }
}
