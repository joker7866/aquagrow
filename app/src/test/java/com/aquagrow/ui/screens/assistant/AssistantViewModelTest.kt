package com.aquagrow.ui.screens.assistant

import androidx.lifecycle.SavedStateHandle
import com.aquagrow.domain.assistant.AssistantQuestion
import com.aquagrow.domain.assistant.AssistantReply
import com.aquagrow.domain.assistant.AssistantRepository
import com.aquagrow.domain.assistant.ChatRole
import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.model.UserPreferences
import com.aquagrow.testing.FakeAquariumRepository
import com.aquagrow.testing.FakeGardenRepository
import com.aquagrow.testing.FakePreferences
import com.aquagrow.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private class ScriptedAssistant(override val isConfigured: Boolean = true) : AssistantRepository {
    val asked = mutableListOf<AssistantQuestion>()
    val replies = ArrayDeque<AssistantReply>()
    override suspend fun ask(question: AssistantQuestion): AssistantReply {
        asked += question
        return replies.removeFirstOrNull() ?: AssistantReply.Answer("ok")
    }
}

class AssistantViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val repo = ScriptedAssistant()
    private fun vm(module: CareModule = CareModule.AQUARIUM, aquariumId: Long? = null, r: AssistantRepository = repo) = AssistantViewModel(
        SavedStateHandle(mapOf("module" to module.name, "plantId" to null, "aquariumId" to aquariumId)),
        r,
        FakeGardenRepository(),
        FakeAquariumRepository(),
        FakePreferences(UserPreferences(onboardingCompleted = true, aiAssistantEnabled = true)),
    )

    @Test fun `answer is shown and kept in the conversation`() = runTest {
        repo.replies += AssistantReply.Answer("Test ammonia.")
        val vm = vm()
        vm.send("Cloudy water?")
        val s = vm.state.first { !it.loading && it.items.size == 2 }
        assertEquals(ChatItem.Answer("Test ammonia.", false), s.items[1])
        vm.send("And then?")
        vm.state.first { it.items.size == 4 }
        val history = repo.asked.last().history
        assertEquals(listOf(ChatRole.USER, ChatRole.ASSISTANT, ChatRole.USER), history.map { it.role })
    }

    @Test fun `rate limit shows a problem and retry re-sends the same question`() = runTest {
        repo.replies += AssistantReply.RateLimited(5)
        val vm = vm()
        vm.send("Hello")
        assertEquals(AssistantProblem.RateLimited(5), vm.state.first { it.problem != null }.problem)
        vm.retry()
        val s = vm.state.first { it.items.size == 2 }
        assertNull(s.problem)
        assertEquals(2, repo.asked.size)
        assertEquals(repo.asked[0], repo.asked[1])
    }

    @Test fun `offline tips are shown but not sent back as conversation`() = runTest {
        repo.replies += AssistantReply.Offline(listOf("Tip"))
        val vm = vm()
        vm.send("First")
        vm.state.first { it.items.size == 2 }
        vm.send("Second")
        vm.state.first { it.items.size == 4 }
        assertEquals(listOf("Second"), repo.asked.last().history.map { it.text })
    }

    @Test fun `opening from a tank preselects it with recent logs`() = runTest {
        val vm = vm(aquariumId = 7)
        val s = vm.state.value
        assertEquals(7L, s.selectedSubject)
        assertTrue(s.includeLogs)
        vm.send("How is my tank?")
        vm.state.first { it.items.size == 2 }
        assertTrue(repo.asked.single().includeRecentLogs)
    }

    @Test fun `not configured is known before asking`() {
        assertTrue(vm(r = ScriptedAssistant(isConfigured = false)).state.value.notConfigured)
    }
}
