package com.aquagrow.domain.repository

import com.aquagrow.domain.model.ModuleChoice
import com.aquagrow.domain.model.ThemeMode
import com.aquagrow.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val preferences: Flow<UserPreferences>

    suspend fun completeOnboarding(choice: ModuleChoice)
    suspend fun setGardenEnabled(enabled: Boolean)
    suspend fun setAquariumEnabled(enabled: Boolean)
    suspend fun setAiAssistantEnabled(enabled: Boolean)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setCurrencyCode(code: String)
    suspend fun setDisplayName(name: String)
}
