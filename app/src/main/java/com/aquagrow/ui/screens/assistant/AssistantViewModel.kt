package com.aquagrow.ui.screens.assistant

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquagrow.domain.assistant.AssistantQuestion
import com.aquagrow.domain.assistant.AssistantReply
import com.aquagrow.domain.assistant.AssistantRepository
import com.aquagrow.domain.assistant.AssistantSubject
import com.aquagrow.domain.assistant.ChatRole
import com.aquagrow.domain.assistant.ChatTurn
import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.repository.AquariumRepository
import com.aquagrow.domain.repository.GardenRepository
import com.aquagrow.domain.repository.UserPreferencesRepository
import com.aquagrow.ui.navigation.AssistantRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One bubble in the chat. */
sealed interface ChatItem {
    data class Question(val text: String, val photoPath: String?) : ChatItem
    data class Answer(val text: String, val truncated: Boolean) : ChatItem
    data class OfflineTips(val tips: List<String>) : ChatItem
}

/** Problem with the last question; shown under the conversation with a retry where it makes sense. */
sealed interface AssistantProblem {
    data class RateLimited(val retryAfterSeconds: Int) : AssistantProblem
    data class Failed(val retryable: Boolean) : AssistantProblem
    data object Declined : AssistantProblem
}

data class SubjectOption(val id: Long, val name: String)

data class AssistantUiState(
    val module: CareModule,
    val enabled: Boolean = true,
    val notConfigured: Boolean = false,
    val items: List<ChatItem> = emptyList(),
    val loading: Boolean = false,
    val problem: AssistantProblem? = null,
    val subjects: List<SubjectOption> = emptyList(),
    val selectedSubject: Long? = null,
    val includeLogs: Boolean = false,
    val photoPath: String? = null,
) {
    val canSend: Boolean get() = enabled && !notConfigured && !loading
}

@HiltViewModel
class AssistantViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AssistantRepository,
    garden: GardenRepository,
    aquarium: AquariumRepository,
    prefs: UserPreferencesRepository,
) : ViewModel() {
    // Arguments of AssistantRoute, read by property name (works without Android's Bundle in unit tests).
    private val plantId: Long? = savedStateHandle.get<Long>(AssistantRoute::plantId.name)
    private val aquariumId: Long? = savedStateHandle.get<Long>(AssistantRoute::aquariumId.name)
    val module: CareModule = CareModule.entries.firstOrNull { it.name == savedStateHandle.get<String>(AssistantRoute::module.name) } ?: CareModule.GARDEN

    private val chat = MutableStateFlow(
        AssistantUiState(module = module, notConfigured = !repository.isConfigured, selectedSubject = plantId ?: aquariumId, includeLogs = plantId != null || aquariumId != null),
    )

    /** Exact turns sent to the backend (answers only; offline tips and errors are not part of the conversation). */
    private val turns = mutableListOf<ChatTurn>()
    private var lastQuestion: AssistantQuestion? = null

    private val subjects = when (module) {
        CareModule.GARDEN -> garden.observePlants().map { l -> l.map { SubjectOption(it.id, listOfNotNull(it.name, it.variety).joinToString(" · ")) } }
        CareModule.AQUARIUM -> aquarium.observeAquariums().map { l -> l.map { SubjectOption(it.id, it.name) } }
    }

    val state: StateFlow<AssistantUiState> = combine(chat, subjects, prefs.preferences) { s, options, p ->
        s.copy(subjects = options, enabled = p.aiAssistantEnabled)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), chat.value)

    fun selectSubject(id: Long?) = chat.update { it.copy(selectedSubject = id, includeLogs = id != null && it.includeLogs) }
    fun setIncludeLogs(on: Boolean) = chat.update { it.copy(includeLogs = on && it.selectedSubject != null) }
    fun attachPhoto(path: String?) = chat.update { it.copy(photoPath = path) }

    fun send(text: String) {
        val question = text.trim()
        val s = chat.value
        if (question.isEmpty() || s.loading) return
        val subject = s.selectedSubject?.let { if (module == CareModule.GARDEN) AssistantSubject.PlantSubject(it) else AssistantSubject.TankSubject(it) }
        turns += ChatTurn(ChatRole.USER, question)
        val q = AssistantQuestion(module, turns.toList(), subject, s.includeLogs, s.photoPath)
        chat.update { it.copy(items = it.items + ChatItem.Question(question, s.photoPath), photoPath = null) }
        ask(q)
    }

    /** Re-sends the last question (after a failure or once a rate limit has passed). */
    fun retry() {
        val q = lastQuestion ?: return
        if (chat.value.loading) return
        ask(q)
    }

    private fun ask(q: AssistantQuestion) {
        lastQuestion = q
        chat.update { it.copy(loading = true, problem = null) }
        viewModelScope.launch {
            val reply = repository.ask(q)
            chat.update { s ->
                when (reply) {
                    is AssistantReply.Answer -> {
                        turns += ChatTurn(ChatRole.ASSISTANT, reply.text)
                        lastQuestion = null
                        s.copy(loading = false, items = s.items + ChatItem.Answer(reply.text, reply.truncated))
                    }

                    is AssistantReply.Offline -> {
                        // Not part of the AI conversation: drop the unanswered question from the turns sent next time.
                        turns.removeLastOrNull()
                        lastQuestion = null
                        s.copy(loading = false, items = s.items + ChatItem.OfflineTips(reply.tips))
                    }

                    is AssistantReply.RateLimited -> s.copy(loading = false, problem = AssistantProblem.RateLimited(reply.retryAfterSeconds))

                    is AssistantReply.Failed -> s.copy(loading = false, problem = AssistantProblem.Failed(reply.retryable))

                    AssistantReply.Declined -> {
                        turns.removeLastOrNull()
                        lastQuestion = null
                        s.copy(loading = false, problem = AssistantProblem.Declined)
                    }

                    AssistantReply.Disabled -> {
                        turns.removeLastOrNull()
                        s.copy(loading = false, enabled = false)
                    }

                    AssistantReply.NotConfigured -> {
                        turns.removeLastOrNull()
                        s.copy(loading = false, notConfigured = true)
                    }
                }
            }
        }
    }
}
