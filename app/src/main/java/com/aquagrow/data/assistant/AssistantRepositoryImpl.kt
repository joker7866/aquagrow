package com.aquagrow.data.assistant

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Base64
import androidx.core.graphics.scale
import com.aquagrow.data.photo.PhotoStorage
import com.aquagrow.domain.assistant.AssistantQuestion
import com.aquagrow.domain.assistant.AssistantReply
import com.aquagrow.domain.assistant.AssistantRepository
import com.aquagrow.domain.assistant.BuildAssistantContextUseCase
import com.aquagrow.domain.assistant.ChatRole
import com.aquagrow.domain.assistant.OfflineTips
import com.aquagrow.domain.repository.UserPreferencesRepository
import com.aquagrow.domain.time.TimeProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

interface ConnectivityChecker {
    fun isOnline(): Boolean
}

class AndroidConnectivityChecker @Inject constructor(@param:ApplicationContext private val context: Context) : ConnectivityChecker {
    override fun isOnline(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}

interface PhotoEncoder {
    /** A downsized JPEG of the photo, or null if it can't be read. */
    suspend fun encode(relativePath: String): ImageDto?
}

class JpegPhotoEncoder @Inject constructor(private val photos: PhotoStorage) : PhotoEncoder {
    override suspend fun encode(relativePath: String): ImageDto? = withContext(Dispatchers.IO) {
        val file = photos.resolve(relativePath)
        if (!file.exists()) return@withContext null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val decoded = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return@withContext null
        val scale = MAX_SIDE.toFloat() / maxOf(decoded.width, decoded.height)
        val bitmap = if (scale < 1f) decoded.scale((decoded.width * scale).toInt(), (decoded.height * scale).toInt()) else decoded
        val bytes = ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            out.toByteArray()
        }
        ImageDto("image/jpeg", Base64.encodeToString(bytes, Base64.NO_WRAP))
    }

    private companion object {
        const val MAX_SIDE = 1024
    }
}

class AssistantRepositoryImpl @Inject constructor(
    private val api: AssistantApi,
    private val connectivity: ConnectivityChecker,
    private val photoEncoder: PhotoEncoder,
    private val prefs: UserPreferencesRepository,
    private val buildContext: BuildAssistantContextUseCase,
    private val offlineTips: OfflineTips,
    private val time: TimeProvider,
) : AssistantRepository {
    override val isConfigured: Boolean get() = api.isConfigured

    override suspend fun ask(question: AssistantQuestion): AssistantReply {
        if (!prefs.preferences.first().aiAssistantEnabled) return AssistantReply.Disabled
        if (!api.isConfigured) return AssistantReply.NotConfigured
        if (!connectivity.isOnline()) return AssistantReply.Offline(offlineTips.forQuestion(question))

        val request = ChatRequestDto(
            assistant = question.module.name.lowercase(),
            messages = trimHistory(question),
            context = buildContext(question.subject, question.includeRecentLogs, time.zone()),
            image = question.photoPath?.let { photoEncoder.encode(it) },
        )

        // One quiet retry for brief network or server hiccups; rate limits are never retried automatically.
        var attempt = 0
        while (true) {
            val reply = send(request, question)
            val retryable = reply is AssistantReply.Failed && reply.retryable
            if (!retryable || attempt >= 1) return reply
            attempt++
            delay(RETRY_DELAY_MS)
        }
    }

    private suspend fun send(request: ChatRequestDto, question: AssistantQuestion): AssistantReply = try {
        val r = api.chat(request)
        when {
            r.refused -> AssistantReply.Declined
            r.reply.isNullOrBlank() -> AssistantReply.Failed(retryable = true)
            else -> AssistantReply.Answer(r.reply.trim(), r.truncated)
        }
    } catch (e: AssistantHttpException) {
        when (e.status) {
            429 -> AssistantReply.RateLimited(e.retryAfterSeconds ?: DEFAULT_RETRY_AFTER_S)
            400, 413 -> AssistantReply.Failed(retryable = false)
            else -> AssistantReply.Failed(retryable = true)
        }
    } catch (e: IOException) {
        if (!connectivity.isOnline()) AssistantReply.Offline(offlineTips.forQuestion(question)) else AssistantReply.Failed(retryable = true)
    }

    companion object {
        const val MAX_TURNS = 12
        const val RETRY_DELAY_MS = 1_500L
        const val DEFAULT_RETRY_AFTER_S = 30

        /** Last [MAX_TURNS] turns, starting with a user turn, as the backend expects. */
        fun trimHistory(q: AssistantQuestion): List<ChatMessageDto> = q.history.filter { it.text.isNotBlank() }
            .takeLast(MAX_TURNS)
            .dropWhile { it.role != ChatRole.USER }
            .map { ChatMessageDto(if (it.role == ChatRole.USER) "user" else "assistant", it.text.take(2_000)) }
    }
}
