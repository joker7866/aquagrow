package com.aquagrow.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AquaGrowShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp), // chips
    medium = RoundedCornerShape(14.dp), // tiles, text fields
    large = RoundedCornerShape(20.dp), // cards
    extraLarge = RoundedCornerShape(28.dp), // sheets, dialogs
)

object Dimens {
    val CardRadius = 20.dp
    val ChipRadius = 12.dp
    val ButtonHeight = 56.dp
    val ButtonHeightCompact = 52.dp
    val MinTouchTarget = 48.dp
    val ScreenPadding = 20.dp
    val CardPadding = 16.dp
    val CardBorderWidth = 1.dp
}
