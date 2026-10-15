package com.aquagrow.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.aquagrow.ui.navigation.TopLevelDestination
import com.aquagrow.ui.preview.ThemePreviews
import com.aquagrow.ui.theme.AquaGrowTheme

@Composable
fun AppBottomNav(
    destinations: List<TopLevelDestination>,
    selected: TopLevelDestination?,
    onSelect: (TopLevelDestination) -> Unit,
) {
    val ext = AquaGrowTheme.colors
    Column {
        HorizontalDivider(color = ext.cardBorder)
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = androidx.compose.ui.unit.Dp(0f)) {
            destinations.forEach { destination ->
                val isSelected = destination == selected
                // Aquarium tab is blue, everything else uses the garden green.
                val module = if (destination == TopLevelDestination.AQUARIUM) ext.aquarium else ext.garden
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onSelect(destination) },
                    icon = {
                        Icon(
                            painter = painterResource(if (isSelected) destination.selectedIcon else destination.icon),
                            contentDescription = null,
                        )
                    },
                    label = {
                        Text(
                            stringResource(destination.label),
                            // Five labels share the width: grow with the font setting, but at most 1.4x,
                            // so they don't truncate at 200%. TalkBack always reads the full label.
                            style = MaterialTheme.typography.labelSmall.let { st ->
                                val scale = LocalDensity.current.fontScale
                                if (scale > MAX_LABEL_SCALE) st.copy(fontSize = st.fontSize * (MAX_LABEL_SCALE / scale)) else st
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = module.primary,
                        selectedTextColor = module.primary,
                        indicatorColor = module.container,
                        unselectedIconColor = ext.mutedText,
                        unselectedTextColor = ext.mutedText,
                    ),
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun AppBottomNavPreview() {
    AquaGrowTheme {
        AppBottomNav(TopLevelDestination.entries, TopLevelDestination.HOME, {})
    }
}

private const val MAX_LABEL_SCALE = 1.4f
