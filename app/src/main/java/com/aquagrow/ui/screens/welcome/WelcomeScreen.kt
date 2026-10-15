package com.aquagrow.ui.screens.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aquagrow.R
import com.aquagrow.ui.components.LeafFishMark
import com.aquagrow.ui.components.PrimaryButton
import com.aquagrow.ui.preview.ScreenPreviews
import com.aquagrow.ui.theme.AquaGrowTheme
import com.aquagrow.ui.theme.Dimens

/** Mockup: beige top with the brand mark, white rounded sheet with the copy and actions. */
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onRestoreBackup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val beige = AquaGrowTheme.colors.garden.surfaceAlt
    // White page so the sheet colour continues to the bottom; the beige area sits on top.
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(beige)
                    .statusBarsPadding()
                    .heightIn(min = 380.dp)
                    .padding(top = 40.dp, bottom = 72.dp),
                contentAlignment = Alignment.Center,
            ) {
                LeafFishMark(ringColor = beige, contentDescription = stringResource(R.string.welcome_illustration))
            }
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-32).dp),
            ) {
                Column(
                    Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = Dimens.ScreenPadding + 8.dp, vertical = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        stringResource(R.string.welcome_eyebrow).uppercase(),
                        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold),
                        color = AquaGrowTheme.colors.garden.primary,
                    )
                    Text(
                        stringResource(R.string.welcome_title),
                        style = MaterialTheme.typography.displaySmall,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        stringResource(R.string.welcome_tagline),
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 26.sp),
                        color = AquaGrowTheme.colors.mutedText,
                    )
                    Spacer(Modifier.height(12.dp))
                    PrimaryButton(stringResource(R.string.welcome_get_started), onGetStarted)
                    TextButton(
                        onClick = onRestoreBackup,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .heightIn(min = Dimens.MinTouchTarget),
                    ) {
                        Text(
                            stringResource(R.string.welcome_restore),
                            style = MaterialTheme.typography.bodyLarge.copy(textDecoration = TextDecoration.Underline),
                            color = AquaGrowTheme.colors.garden.primary,
                        )
                    }
                }
            }
        }
    }
}

@ScreenPreviews
@Composable
private fun WelcomeScreenPreview() {
    AquaGrowTheme { WelcomeScreen(onGetStarted = {}, onRestoreBackup = {}) }
}
