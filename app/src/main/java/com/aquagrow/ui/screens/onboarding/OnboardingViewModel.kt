package com.aquagrow.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquagrow.domain.model.ModuleChoice
import com.aquagrow.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val selected: ModuleChoice? = null,
    val saving: Boolean = false,
    /** Set once the choice is saved; the screen then navigates on. */
    val finished: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun select(choice: ModuleChoice) = _state.update { it.copy(selected = choice) }

    fun onContinue() {
        val choice = _state.value.selected ?: return
        save(choice)
    }

    /** Skip = keep both modules on; they can be switched off later in Profile. */
    fun onSkip() = save(ModuleChoice.BOTH)

    private fun save(choice: ModuleChoice) {
        if (_state.value.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            prefs.completeOnboarding(choice)
            _state.update { it.copy(saving = false, finished = true) }
        }
    }
}
