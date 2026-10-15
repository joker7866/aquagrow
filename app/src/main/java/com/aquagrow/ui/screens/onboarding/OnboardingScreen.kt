package com.aquagrow.ui.screens.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aquagrow.R
import com.aquagrow.domain.model.ModuleChoice
import com.aquagrow.ui.components.PrimaryButton
import com.aquagrow.ui.components.TipCard
import com.aquagrow.ui.preview.ScreenPreviews
import com.aquagrow.ui.theme.AquaGrowTheme
import com.aquagrow.ui.theme.Dimens

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.finished) { if (state.finished) onFinished() }
    OnboardingContent(
        state = state,
        onSelect = viewModel::select,
        onContinue = viewModel::onContinue,
        onSkip = viewModel::onSkip,
        modifier = modifier,
    )
}

/** Total onboarding steps: 1 = modules, 2 = reminders permission. */
const val ONBOARDING_STEPS = 2

@Composable
fun OnboardingContent(
    state: OnboardingUiState,
    onSelect: (ModuleChoice) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier
                .systemBarsPadding()
                .padding(horizontal = Dimens.ScreenPadding + 4.dp),
        ) {
            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
                ProgressDots(current = 1, total = ONBOARDING_STEPS, modifier = Modifier.weight(1f))
                TextButton(onClick = onSkip, enabled = !state.saving) {
                    Text(stringResource(R.string.action_skip), style = MaterialTheme.typography.bodyLarge, color = AquaGrowTheme.colors.mutedText)
                }
            }

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(20.dp))
                Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.onboarding_subtitle), style = MaterialTheme.typography.bodyLarge, color = AquaGrowTheme.colors.mutedText)
                Spacer(Modifier.height(28.dp))

                val ext = AquaGrowTheme.colors
                Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ChoiceCard(
                        title = stringResource(R.string.onboarding_garden_title),
                        body = stringResource(R.string.onboarding_garden_body),
                        icon = R.drawable.ic_sprout,
                        iconTint = ext.garden.primary,
                        tileColor = ext.garden.container,
                        selected = state.selected == ModuleChoice.GARDEN,
                        onClick = { onSelect(ModuleChoice.GARDEN) },
                    )
                    ChoiceCard(
                        title = stringResource(R.string.onboarding_aquarium_title),
                        body = stringResource(R.string.onboarding_aquarium_body),
                        icon = R.drawable.ic_fish,
                        iconTint = ext.aquarium.primary,
                        tileColor = ext.aquarium.container,
                        selected = state.selected == ModuleChoice.AQUARIUM,
                        onClick = { onSelect(ModuleChoice.AQUARIUM) },
                    )
                    ChoiceCard(
                        title = stringResource(R.string.onboarding_both_title),
                        body = stringResource(R.string.onboarding_both_body),
                        icon = R.drawable.ic_join,
                        iconTint = MaterialTheme.colorScheme.onSurface,
                        tileColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        selected = state.selected == ModuleChoice.BOTH,
                        onClick = { onSelect(ModuleChoice.BOTH) },
                    )
                }
                Spacer(Modifier.height(24.dp))
                TipCard(title = null, body = stringResource(R.string.onboarding_note), showIcon = false)
                Spacer(Modifier.height(24.dp))
            }

            PrimaryButton(
                text = stringResource(R.string.action_continue),
                onClick = onContinue,
                enabled = state.selected != null && !state.saving,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
    }
}

@Composable
private fun ChoiceCard(
    title: String,
    body: String,
    @DrawableRes icon: Int,
    iconTint: Color,
    tileColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val garden = AquaGrowTheme.colors.garden
    val borderColor by animateColorAsState(if (selected) garden.primary else AquaGrowTheme.colors.cardBorder, label = "choice-border")
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (selected) garden.container.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Surface(shape = MaterialTheme.shapes.medium, color = tileColor, modifier = Modifier.size(56.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(painterResource(icon), null, tint = iconTint, modifier = Modifier.size(30.dp)) }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = AquaGrowTheme.colors.mutedText)
            }
            if (selected) {
                Surface(shape = CircleShape, color = garden.primary, modifier = Modifier.size(28.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_check), null, tint = garden.onPrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressDots(current: Int, total: Int, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.onboarding_progress, current, total)
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { index ->
            Surface(
                shape = CircleShape,
                color = if (index < current) AquaGrowTheme.colors.garden.primary else AquaGrowTheme.colors.cardBorder,
                modifier = Modifier.height(8.dp).width(28.dp),
            ) {}
        }
    }
}

@ScreenPreviews
@Composable
private fun OnboardingPreview() {
    AquaGrowTheme { OnboardingContent(OnboardingUiState(selected = ModuleChoice.BOTH), {}, {}, {}) }
}
