package com.aquagrow.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.aquagrow.domain.model.ModuleChoice
import com.aquagrow.domain.model.ThemeMode
import com.aquagrow.domain.model.UserPreferences
import com.aquagrow.domain.repository.UserPreferencesRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

@Singleton
class DataStoreUserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val GARDEN_ENABLED = booleanPreferencesKey("garden_enabled")
        val AQUARIUM_ENABLED = booleanPreferencesKey("aquarium_enabled")
        val AI_ASSISTANT_ENABLED = booleanPreferencesKey("ai_assistant_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
    }

    override val preferences: Flow<UserPreferences> = dataStore.data
        // A corrupt/unreadable file falls back to defaults instead of crashing the app.
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            val defaults = UserPreferences()
            UserPreferences(
                onboardingCompleted = p[Keys.ONBOARDING_COMPLETED] ?: defaults.onboardingCompleted,
                gardenEnabled = p[Keys.GARDEN_ENABLED] ?: defaults.gardenEnabled,
                aquariumEnabled = p[Keys.AQUARIUM_ENABLED] ?: defaults.aquariumEnabled,
                aiAssistantEnabled = p[Keys.AI_ASSISTANT_ENABLED] ?: defaults.aiAssistantEnabled,
                themeMode = p[Keys.THEME_MODE]
                    ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                    ?: defaults.themeMode,
                currencyCode = p[Keys.CURRENCY_CODE] ?: defaults.currencyCode,
                displayName = p[Keys.DISPLAY_NAME] ?: defaults.displayName,
            )
        }

    override suspend fun completeOnboarding(choice: ModuleChoice) {
        dataStore.edit {
            it[Keys.GARDEN_ENABLED] = choice.garden
            it[Keys.AQUARIUM_ENABLED] = choice.aquarium
            it[Keys.ONBOARDING_COMPLETED] = true
        }
    }

    override suspend fun setGardenEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.GARDEN_ENABLED] = enabled }
    }

    override suspend fun setAquariumEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.AQUARIUM_ENABLED] = enabled }
    }

    override suspend fun setAiAssistantEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.AI_ASSISTANT_ENABLED] = enabled }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    override suspend fun setCurrencyCode(code: String) {
        dataStore.edit { it[Keys.CURRENCY_CODE] = code }
    }

    override suspend fun setDisplayName(name: String) {
        dataStore.edit { it[Keys.DISPLAY_NAME] = name.trim().take(40) }
    }
}
