package com.aquagrow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.model.ThemeMode

private val LightColors = lightColorScheme(
    primary = GardenPrimary,
    onPrimary = Color.White,
    primaryContainer = GardenContainer,
    onPrimaryContainer = LightGardenColors.onContainer,
    secondary = AquariumPrimary,
    onSecondary = Color.White,
    secondaryContainer = AquariumContainer,
    onSecondaryContainer = LightAquariumColors.onContainer,
    tertiary = GardenSage,
    background = PageBackground,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = GardenBeige,
    onSurfaceVariant = MutedText,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF4F3EF),
    surfaceContainerHighest = Color(0xFFEDECE8),
    outline = Color(0xFF8E9690),
    outlineVariant = CardBorder,
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

private val DarkColors = darkColorScheme(
    primary = DarkGardenPrimary,
    onPrimary = DarkOnGardenPrimary,
    primaryContainer = DarkGardenContainer,
    onPrimaryContainer = DarkOnGardenContainer,
    secondary = DarkAquariumPrimary,
    onSecondary = DarkOnAquariumPrimary,
    secondaryContainer = DarkAquariumContainer,
    onSecondaryContainer = DarkOnAquariumContainer,
    tertiary = DarkGardenAccent,
    background = DarkPageBackground,
    onBackground = DarkInk,
    surface = DarkCard,
    onSurface = DarkInk,
    surfaceVariant = Color(0xFF252B27),
    onSurfaceVariant = DarkMutedText,
    surfaceContainerLowest = DarkPageBackground,
    surfaceContainerLow = DarkCard,
    surfaceContainer = DarkCard,
    surfaceContainerHigh = Color(0xFF232925),
    surfaceContainerHighest = Color(0xFF2B312D),
    outline = Color(0xFF8A938D),
    outlineVariant = DarkCardBorder,
    error = Color(0xFFF2B8B5),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

/** Colours of the module the current screen belongs to (green garden / blue aquarium). */
val LocalModuleColors = staticCompositionLocalOf { LightGardenColors }

@Composable
fun AquaGrowTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val extended = if (dark) DarkExtendedColors else LightExtendedColors
    CompositionLocalProvider(
        LocalExtendedColors provides extended,
        LocalModuleColors provides extended.garden,
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = AquaGrowTypography,
            shapes = AquaGrowShapes,
            content = content,
        )
    }
}

/** Switches [LocalModuleColors] for a subtree, e.g. every Aquarium screen. */
@Composable
fun ModuleTheme(module: CareModule, content: @Composable () -> Unit) {
    val ext = LocalExtendedColors.current
    val colors = if (module == CareModule.GARDEN) ext.garden else ext.aquarium
    CompositionLocalProvider(LocalModuleColors provides colors) {
        // Material components inside the module (FABs, switches, chips) pick up the module colour.
        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme.copy(
                primary = colors.primary,
                onPrimary = colors.onPrimary,
                primaryContainer = colors.container,
                onPrimaryContainer = colors.onContainer,
            ),
            content = content,
        )
    }
}

/** Shorthand accessors: `AquaGrowTheme.colors.cardBorder`, `AquaGrowTheme.module.primary`. */
object AquaGrowTheme {
    val colors: ExtendedColors
        @Composable @ReadOnlyComposable
        get() = LocalExtendedColors.current

    val module: ModuleColors
        @Composable @ReadOnlyComposable
        get() = LocalModuleColors.current
}

fun ExtendedColors.forModule(module: CareModule): ModuleColors = if (module == CareModule.GARDEN) garden else aquarium
