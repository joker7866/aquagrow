package com.aquagrow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aquagrow.R
import com.aquagrow.ui.preview.ThemePreviews
import com.aquagrow.ui.theme.AquaGrowTheme

/**
 * Welcome mark (mockup): a sage circle with a sprout, overlapped bottom-right by an aqua
 * circle with a fish. A ring in [ringColor] separates the two where they overlap.
 */
@Composable
fun LeafFishMark(
    modifier: Modifier = Modifier,
    size: Dp = 170.dp,
    ringColor: Color = MaterialTheme.colorScheme.background,
    contentDescription: String? = null,
) {
    val garden = AquaGrowTheme.colors.garden
    val aquarium = AquaGrowTheme.colors.aquarium
    Box(
        modifier = modifier
            .size(width = size * 1.55f, height = size * 1.35f)
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier),
    ) {
        Circle(size, garden.container, Modifier.align(Alignment.TopStart)) {
            Icon(
                painterResource(R.drawable.ic_sprout),
                null,
                tint = garden.primary,
                modifier = Modifier
                    .size(size * 0.5f)
                    .offset(x = size * 0.05f, y = -size * 0.08f),
            )
        }
        Circle(
            size,
            aquarium.accent,
            Modifier
                .align(Alignment.BottomEnd)
                .border(8.dp, ringColor, CircleShape),
        ) {
            Icon(painterResource(R.drawable.ic_fish_eye), null, tint = aquarium.primary, modifier = Modifier.size(size * 0.48f))
        }
    }
}

@Composable
private fun Circle(size: Dp, color: Color, modifier: Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/**
 * Home empty-state illustration (mockup): a terracotta pot with a sprout next to a small
 * aquarium with a fish.
 */
@Composable
fun PotAndTankIllustration(modifier: Modifier = Modifier) {
    val garden = AquaGrowTheme.colors.garden
    val aquarium = AquaGrowTheme.colors.aquarium
    Box(modifier.size(width = 220.dp, height = 150.dp)) {
        // Pot
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp)
                .size(width = 100.dp, height = 90.dp)
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 28.dp, bottomEnd = 4.dp))
                .background(PotColor),
        )
        Icon(
            painterResource(R.drawable.ic_sprout),
            null,
            tint = garden.primary,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 38.dp)
                .size(64.dp),
        )
        // Tank
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = aquarium.accent,
            border = androidx.compose.foundation.BorderStroke(4.dp, aquarium.primary),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp)
                .size(width = 108.dp, height = 96.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_fish), null, tint = aquarium.primary, modifier = Modifier.size(44.dp))
            }
        }
    }
}

private val PotColor = Color(0xFFC9A27C)

@ThemePreviews
@Composable
private fun IllustrationsPreview() {
    AquaGrowTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            androidx.compose.foundation.layout.Column(Modifier.padding(16.dp)) {
                LeafFishMark()
                PotAndTankIllustration()
            }
        }
    }
}
