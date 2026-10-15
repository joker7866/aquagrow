package com.aquagrow.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// ---- Raw palette (light values come straight from the design spec) ----

// Shared
internal val Ink = Color(0xFF1C2421)
internal val MutedText = Color(0xFF5B6660)
internal val PageBackground = Color(0xFFFAF8F4)
internal val CardBorder = Color(0xFFECEDEA)
internal val WarningContainer = Color(0xFFFCEBD9)
internal val OnWarningContainer = Color(0xFF8A4B0F)
internal val SuccessText = Color(0xFF1B6B3A)

// Garden
internal val GardenPrimary = Color(0xFF2F6B3A)
internal val GardenContainer = Color(0xFFE3ECE1)
internal val GardenSage = Color(0xFF7A9E7E)
internal val GardenBeige = Color(0xFFF4EFE6)
internal val GardenPageBackground = Color(0xFFF7F4EE)

// Aquarium
internal val AquariumPrimary = Color(0xFF0B5D8C)
internal val AquariumContainer = Color(0xFFDDF0F4)
internal val AquariumAqua = Color(0xFFBFE4EA)
internal val AquariumPageBackground = Color(0xFFF2F8FA)

// Dark counterparts. Each foreground/background pair below keeps >= 4.5:1 contrast.
internal val DarkInk = Color(0xFFE6E9E6)
internal val DarkMutedText = Color(0xFFAEB8B1)
internal val DarkPageBackground = Color(0xFF121614)
internal val DarkCard = Color(0xFF1B201D)
internal val DarkCardBorder = Color(0xFF2E3531)
internal val DarkWarningContainer = Color(0xFF4A2E12)
internal val DarkOnWarningContainer = Color(0xFFF7CFA3)
internal val DarkSuccessText = Color(0xFF8FD6A4)

internal val DarkGardenPrimary = Color(0xFF94D09E)
internal val DarkOnGardenPrimary = Color(0xFF0B3A16)
internal val DarkGardenContainer = Color(0xFF223A27)
internal val DarkOnGardenContainer = Color(0xFFD3E8D5)
internal val DarkGardenAccent = Color(0xFF6F9473)
internal val DarkGardenSurfaceAlt = Color(0xFF242A24)
internal val DarkGardenPageBackground = Color(0xFF131713)

internal val DarkAquariumPrimary = Color(0xFF85C8F0)
internal val DarkOnAquariumPrimary = Color(0xFF00324D)
internal val DarkAquariumContainer = Color(0xFF15394B)
internal val DarkOnAquariumContainer = Color(0xFFCFEAF4)
internal val DarkAquariumAccent = Color(0xFF3E7A86)
internal val DarkAquariumSurfaceAlt = Color(0xFF1C2A2F)
internal val DarkAquariumPageBackground = Color(0xFF101719)

/** Colours for one care module. Screens read these via `AquaGrowTheme.module`. */
@Immutable
data class ModuleColors(
    val primary: Color,
    val onPrimary: Color,
    val container: Color,
    val onContainer: Color,
    /** Sage (garden) / aqua (aquarium): illustrations, chart tracks, decorative fills. */
    val accent: Color,
    /** Beige (garden) / aqua-tinted (aquarium) secondary surface. */
    val surfaceAlt: Color,
    val pageBackground: Color,
)

internal val LightGardenColors = ModuleColors(
    primary = GardenPrimary,
    onPrimary = Color.White,
    container = GardenContainer,
    onContainer = Color(0xFF1E4A27),
    accent = GardenSage,
    surfaceAlt = GardenBeige,
    pageBackground = GardenPageBackground,
)

internal val DarkGardenColors = ModuleColors(
    primary = DarkGardenPrimary,
    onPrimary = DarkOnGardenPrimary,
    container = DarkGardenContainer,
    onContainer = DarkOnGardenContainer,
    accent = DarkGardenAccent,
    surfaceAlt = DarkGardenSurfaceAlt,
    pageBackground = DarkGardenPageBackground,
)

internal val LightAquariumColors = ModuleColors(
    primary = AquariumPrimary,
    onPrimary = Color.White,
    container = AquariumContainer,
    onContainer = Color(0xFF083F60),
    accent = AquariumAqua,
    surfaceAlt = AquariumContainer,
    pageBackground = AquariumPageBackground,
)

internal val DarkAquariumColors = ModuleColors(
    primary = DarkAquariumPrimary,
    onPrimary = DarkOnAquariumPrimary,
    container = DarkAquariumContainer,
    onContainer = DarkOnAquariumContainer,
    accent = DarkAquariumAccent,
    surfaceAlt = DarkAquariumSurfaceAlt,
    pageBackground = DarkAquariumPageBackground,
)

/** App-wide colours that Material's ColorScheme has no slot for. */
@Immutable
data class ExtendedColors(
    val cardBorder: Color,
    val mutedText: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val success: Color,
    val garden: ModuleColors,
    val aquarium: ModuleColors,
)

internal val LightExtendedColors = ExtendedColors(
    cardBorder = CardBorder,
    mutedText = MutedText,
    warningContainer = WarningContainer,
    onWarningContainer = OnWarningContainer,
    success = SuccessText,
    garden = LightGardenColors,
    aquarium = LightAquariumColors,
)

internal val DarkExtendedColors = ExtendedColors(
    cardBorder = DarkCardBorder,
    mutedText = DarkMutedText,
    warningContainer = DarkWarningContainer,
    onWarningContainer = DarkOnWarningContainer,
    success = DarkSuccessText,
    garden = DarkGardenColors,
    aquarium = DarkAquariumColors,
)
