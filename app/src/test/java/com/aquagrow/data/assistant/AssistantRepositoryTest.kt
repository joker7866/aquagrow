package com.aquagrow.data.assistant

import com.aquagrow.domain.assistant.AssistantQuestion
import com.aquagrow.domain.assistant.AssistantReply
import com.aquagrow.domain.assistant.AssistantSubject
import com.aquagrow.domain.assistant.BuildAssistantContextUseCase
import com.aquagrow.domain.assistant.ChatRole
import com.aquagrow.domain.assistant.ChatTurn
import com.aquagrow.domain.assistant.OfflineTips
import com.aquagrow.domain.model.Aquarium
import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.model.Fish
import com.aquagrow.domain.model.Plant
import com.aquagrow.domain.model.PlantCategory
import com.aquagrow.domain.model.UserPreferences
import com.aquagrow.domain.model.WaterTest
import com.aquagrow.testing.FakeAquariumRepository
import com.aquagrow.testing.FakeGardenRepository
import com.aquagrow.testing.FakePreferences
import com.aquagrow.testing.FakeTime
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Scripted backend: returns or throws the queued results in order and records requests. */
private class FakeAssistantApi(override val isConfigured: Boolean = true) : AssistantApi {
    val requests = mutableListOf<ChatRequestDto>()
    val results = ArrayDeque<Result<ChatResponseDto>>()

    override suspend fun chat(request: ChatRequestDto): ChatResponseDto {
        requests += request
        return (results.removeFirstOrNull() ?: Result.success(ChatResponseDto(reply = "ok"))).getOrThrow()
    }
}

private class FakeConnectivity(var online: Boolean = true) : ConnectivityChecker {
    override fun isOnline() = online
}

private class FakePhotoEncoder : PhotoEncoder {
    override suspend fun encode(relativePath: String) = ImageDto("image/jpeg", "BASE64:$relativePath")
}

class AssistantRepositoryTest {
    private val api = FakeAssistantApi()
    private val net = FakeConnectivity()
    private val prefs = FakePreferences(UserPreferences(onboardingCompleted = true, aiAssistantEnabled = true))
    private val garden = FakeGardenRepository()
    private val aquarium = FakeAquariumRepository()
    private val repo = AssistantRepositoryImpl(
        api,
        net,
        FakePhotoEncoder(),
        prefs,
        BuildAssistantContextUseCase(garden, aquarium),
        OfflineTips(garden, aquarium),
        FakeTime(),
    )

    private fun question(text: String = "Why is my water cloudy?", module: CareModule = CareModule.AQUARIUM, subject: AssistantSubject? = null, logs: Boolean = false, photo: String? = null) = AssistantQuestion(module, listOf(ChatTurn(ChatRole.USER, text)), subject, logs, photo)

    private val tank = Aquarium(id = 7, name = "Living room", volumeLiters = 72.0)

    @Test fun `answer is returned and the request carries module and question`() = runTest {
        api.results += Result.success(ChatResponseDto(reply = "  Check ammonia first. "))
        val reply = repo.ask(question())
        assertEquals(AssistantReply.Answer("Check ammonia first."), reply)
        val sent = api.requests.single()
        assertEquals("aquarium", sent.assistant)
        assertEquals(listOf(ChatMessageDto("user", "Why is my water cloudy?")), sent.messages)
        assertNull("nothing attached unless chosen", sent.context)
        assertNull(sent.image)
    }

    @Test fun `disabled assistant never calls the backend`() = runTest {
        prefs.state.value = prefs.state.value.copy(aiAssistantEnabled = false)
        assertEquals(AssistantReply.Disabled, repo.ask(question()))
        assertTrue(api.requests.isEmpty())
    }

    @Test fun `missing backend url is reported as not configured`() = runTest {
        val r = AssistantRepositoryImpl(
            FakeAssistantApi(isConfigured = false),
            net,
            FakePhotoEncoder(),
            prefs,
            BuildAssistantContextUseCase(garden, aquarium),
            OfflineTips(garden, aquarium),
            FakeTime(),
        )
        assertFalse(r.isConfigured)
        assertEquals(AssistantReply.NotConfigured, r.ask(question()))
    }

    @Test fun `offline falls back to catalog tips for the attached tank`() = runTest {
        net.online = false
        aquarium.aquariums.value = listOf(tank)
        aquarium.fish.value = listOf(Fish(id = 1, aquariumId = 7, speciesKey = "neon_tetra", speciesName = "Neon tetra", count = 8))
        val reply = repo.ask(question(subject = AssistantSubject.TankSubject(7)))
        assertTrue(reply is AssistantReply.Offline)
        val tips = (reply as AssistantReply.Offline).tips
        assertTrue(tips.isNotEmpty())
        assertTrue("species tips come first", tips.first().startsWith("Neon"))
        assertTrue(api.requests.isEmpty())
    }

    @Test fun `offline garden tips never give a fixed watering interval`() = runTest {
        net.online = false
        val tips = (repo.ask(question(module = CareModule.GARDEN)) as AssistantReply.Offline).tips
        assertTrue(tips.any { "soil" in it })
        assertFalse(tips.any { Regex("every \\d+ days?").containsMatchIn(it) })
    }

    @Test fun `rate limit is passed on with retry-after and not retried automatically`() = runTest {
        api.results += Result.failure(AssistantHttpException(429, 42))
        assertEquals(AssistantReply.RateLimited(42), repo.ask(question()))
        assertEquals(1, api.requests.size)
    }

    @Test fun `server hiccup is retried once, then succeeds`() = runTest {
        api.results += Result.failure(AssistantHttpException(503, null))
        api.results += Result.success(ChatResponseDto(reply = "Second time lucky"))
        assertEquals(AssistantReply.Answer("Second time lucky"), repo.ask(question()))
        assertEquals(2, api.requests.size)
    }

    @Test fun `repeated failure gives a retryable error after two attempts`() = runTest {
        api.results += Result.failure(IOException("reset"))
        api.results += Result.failure(IOException("reset"))
        assertEquals(AssistantReply.Failed(retryable = true), repo.ask(question()))
        assertEquals(2, api.requests.size)
    }

    @Test fun `bad request is not retryable`() = runTest {
        api.results += Result.failure(AssistantHttpException(400, null))
        assertEquals(AssistantReply.Failed(retryable = false), repo.ask(question()))
        assertEquals(1, api.requests.size)
    }

    @Test fun `declined question is reported`() = runTest {
        api.results += Result.success(ChatResponseDto(reply = null, refused = true))
        assertEquals(AssistantReply.Declined, repo.ask(question()))
    }

    @Test fun `attached tank and logs become context, photo is encoded`() = runTest {
        aquarium.aquariums.value = listOf(tank)
        aquarium.tests.value = listOf(WaterTest(id = 1, aquariumId = 7, testedAt = Instant.parse("2026-10-01T06:00:00Z"), ph = 7.2, nitratePpm = 40.0))
        repo.ask(question(subject = AssistantSubject.TankSubject(7), logs = true, photo = "photos/x.jpg"))
        val sent = api.requests.single()
        val ctx = sent.context!!
        assertTrue(ctx.contains("Living room"))
        assertTrue(ctx.contains("72 L"))
        assertTrue(ctx.contains("nitrate 40.0 ppm"))
        assertEquals(ImageDto("image/jpeg", "BASE64:photos/x.jpg"), sent.image)
    }

    @Test fun `logs are left out unless chosen`() = runTest {
        garden.plants.value = listOf(Plant(id = 3, name = "Tomato", category = PlantCategory.VEGETABLE, plantingDate = LocalDate.of(2026, 9, 1), growthStages = listOf("Seedling")))
        repo.ask(question(module = CareModule.GARDEN, subject = AssistantSubject.PlantSubject(3), logs = false))
        val ctx = api.requests.single().context!!
        assertTrue(ctx.contains("Tomato"))
        assertFalse(ctx.contains("Recent"))
    }

    @Test fun `history is trimmed to recent turns and starts with the user`() {
        val turns = (1..20).map { ChatTurn(if (it % 2 == 1) ChatRole.USER else ChatRole.ASSISTANT, "t$it") } + ChatTurn(ChatRole.USER, "last")
        val trimmed = AssistantRepositoryImpl.trimHistory(AssistantQuestion(CareModule.GARDEN, turns))
        assertTrue(trimmed.size <= AssistantRepositoryImpl.MAX_TURNS)
        assertEquals("user", trimmed.first().role)
        assertEquals("last", trimmed.last().text)
    }
}
