package com.aquagrow.domain.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** What the user picked during onboarding. */
enum class ModuleChoice(val garden: Boolean, val aquarium: Boolean) {
    GARDEN(garden = true, aquarium = false),
    AQUARIUM(garden = false, aquarium = true),
    BOTH(garden = true, aquarium = true),
}

data class UserPreferences(
    val onboardingCompleted: Boolean = false,
    val gardenEnabled: Boolean = true,
    val aquariumEnabled: Boolean = true,
    val aiAssistantEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val currencyCode: String = DEFAULT_CURRENCY,
    /** Optional, only used for the Profile avatar initial. Never leaves the phone. */
    val displayName: String = "",
) {
    companion object {
        const val DEFAULT_CURRENCY = "INR"
    }
}
