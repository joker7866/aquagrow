package com.aquagrow.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.aquagrow.R
import com.aquagrow.ui.preview.ThemePreviews
import com.aquagrow.ui.theme.AquaGrowTheme
import com.aquagrow.ui.theme.Dimens

enum class ChipTone { GARDEN, AQUA, WARNING, NEUTRAL, SUCCESS }

@Composable
fun StatusChip(
    text: String,
    tone: ChipTone,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
) {
    val (bg, fg) = chipColors(tone)
    Surface(modifier = modifier, shape = RoundedCornerShape(Dimens.ChipRadius - 2.dp), color = bg, contentColor = fg) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (icon != null) Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(14.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

@Composable
private fun chipColors(tone: ChipTone): Pair<Color, Color> {
    val c = AquaGrowTheme.colors
    return when (tone) {
        ChipTone.GARDEN -> c.garden.container to c.garden.onContainer
        ChipTone.AQUA -> c.aquarium.container to c.aquarium.onContainer
        ChipTone.WARNING -> c.warningContainer to c.onWarningContainer
        ChipTone.SUCCESS -> c.garden.container to c.success
        ChipTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@ThemePreviews
@Composable
private fun StatusChipPreview() {
    AquaGrowTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip("Seedling", ChipTone.GARDEN)
                StatusChip("Stable", ChipTone.AQUA)
                StatusChip("Due today", ChipTone.WARNING, icon = R.drawable.ic_water_drop)
                StatusChip("Estimate", ChipTone.NEUTRAL)
            }
        }
    }
}
