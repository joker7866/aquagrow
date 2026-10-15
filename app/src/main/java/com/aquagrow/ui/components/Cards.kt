package com.aquagrow.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aquagrow.R
import com.aquagrow.ui.preview.ThemePreviews
import com.aquagrow.ui.theme.AquaGrowTheme
import com.aquagrow.ui.theme.Dimens
import com.aquagrow.ui.theme.ModuleColors

/** White card, 1dp border, 20dp corners — the base surface of the design system. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = MaterialTheme.colorScheme.surface,
    contentPadding: Dp = Dimens.CardPadding,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    val border = BorderStroke(Dimens.CardBorderWidth, AquaGrowTheme.colors.cardBorder)
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = color, border = border) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    } else {
        Surface(modifier = modifier, shape = shape, color = color, border = border) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

/** Rounded square holding an icon, tinted with a module's colours. */
@Composable
fun IconTile(
    @DrawableRes icon: Int,
    colors: ModuleColors,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    contentDescription: String? = null,
) {
    Surface(
        modifier = modifier.size(size),
        shape = MaterialTheme.shapes.medium,
        color = colors.container,
        contentColor = colors.onContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(icon),
                contentDescription = contentDescription,
                tint = colors.primary,
                modifier = Modifier.size(size * 0.5f),
            )
        }
    }
}

@Composable
fun StatCard(
    @DrawableRes icon: Int,
    value: String,
    label: String,
    colors: ModuleColors,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    onClick: (() -> Unit)? = null,
) {
    AppCard(modifier = modifier, onClick = onClick) {
        IconTile(icon = icon, colors = colors, size = 40.dp)
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        if (supporting != null) {
            Text(supporting, style = MaterialTheme.typography.bodySmall, color = AquaGrowTheme.colors.mutedText)
        }
    }
}

/** Mockup Home stat card: filled with the module container colour, no border, no icon. */
@Composable
fun FilledStatCard(
    label: String,
    value: String,
    colors: ModuleColors,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val content: @Composable () -> Unit = {
        Column(Modifier.padding(Dimens.CardPadding + 4.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = colors.primary)
            Text(value, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(vertical = 4.dp))
            if (supporting != null) Text(supporting, style = MaterialTheme.typography.bodyMedium, color = colors.onContainer)
        }
    }
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = MaterialTheme.shapes.large, color = colors.container) { content() }
    } else {
        Surface(modifier = modifier, shape = MaterialTheme.shapes.large, color = colors.container) { content() }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.MinTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        if (trailingText != null) {
            Text(trailingText, style = MaterialTheme.typography.labelMedium, color = AquaGrowTheme.colors.mutedText)
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel, style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@ThemePreviews
@androidx.compose.runtime.Composable
private fun CardsPreview() {
    AquaGrowTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader("Today", trailingText = "3 left")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(R.drawable.ic_sprout, "12", "Plants", AquaGrowTheme.colors.garden, Modifier.weight(1f))
                    StatCard(R.drawable.ic_fish, "18", "Fish", AquaGrowTheme.colors.aquarium, Modifier.weight(1f), "in 2 tanks")
                }
            }
        }
    }
}
