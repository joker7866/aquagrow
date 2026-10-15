package com.aquagrow.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aquagrow.R
import com.aquagrow.ui.preview.ThemePreviews
import com.aquagrow.ui.theme.AquaGrowTheme
import com.aquagrow.ui.theme.ModuleColors

/** Square-ish tile used by Quick add (Home) and the tool rows (Garden / Aquarium). */
@Composable
fun QuickActionTile(
    @DrawableRes icon: Int,
    label: String,
    colors: ModuleColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Tool tiles show the icon on a tinted tile; Quick add shows the bare icon (mockup). */
    iconTile: Boolean = true,
) {
    AppCard(modifier = modifier.heightIn(min = 96.dp), onClick = onClick, contentPadding = 12.dp) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            if (iconTile) {
                IconTile(icon = icon, colors = colors, size = 44.dp)
            } else {
                Icon(painterResource(icon), null, tint = colors.primary, modifier = Modifier.size(28.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Centered illustration + title + body + actions. */
@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    illustration: @Composable () -> Unit = {},
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        illustration()
        Spacer(Modifier.height(20.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = AquaGrowTheme.colors.mutedText,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 360.dp),
        )
        Spacer(Modifier.height(24.dp))
        Column(Modifier.widthIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = actions)
    }
}

/** Shown wherever a plant/fish/tank has no photo yet. */
@Composable
fun PhotoPlaceholder(
    @DrawableRes icon: Int,
    colors: ModuleColors,
    modifier: Modifier = Modifier,
    iconSize: Dp = 32.dp,
    shape: androidx.compose.ui.graphics.Shape = MaterialTheme.shapes.medium,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = colors.primary.copy(alpha = 0.7f), modifier = Modifier.size(iconSize))
    }
}

/** Beige tip / info card (mockup: Home empty state, onboarding note). */
@Composable
fun TipCard(title: String?, body: String, modifier: Modifier = Modifier, showIcon: Boolean = true) {
    Surface(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = AquaGrowTheme.colors.garden.surfaceAlt) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (showIcon) Icon(painterResource(R.drawable.ic_lightbulb), contentDescription = null, tint = TipIconColor)
            Column {
                if (title != null) Text(title, style = MaterialTheme.typography.titleSmall)
                Text(body, style = MaterialTheme.typography.bodyLarge, color = AquaGrowTheme.colors.mutedText)
            }
        }
    }
}

private val TipIconColor = androidx.compose.ui.graphics.Color(0xFFA0702A)

@ThemePreviews
@Composable
private fun TilesPreview() {
    AquaGrowTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickActionTile(R.drawable.ic_add, "Add plant", AquaGrowTheme.colors.garden, {}, Modifier.weight(1f))
                    QuickActionTile(R.drawable.ic_fish, "Log feeding", AquaGrowTheme.colors.aquarium, {}, Modifier.weight(1f))
                    QuickActionTile(R.drawable.ic_add_task, "Add task", AquaGrowTheme.colors.garden, {}, Modifier.weight(1f))
                }
                PhotoPlaceholder(R.drawable.ic_sprout, AquaGrowTheme.colors.garden, Modifier.size(72.dp))
                TipCard("Tip", "Check the soil with your finger before watering.")
            }
        }
    }
}
