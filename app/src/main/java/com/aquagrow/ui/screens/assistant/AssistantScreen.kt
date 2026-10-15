package com.aquagrow.ui.screens.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aquagrow.R
import com.aquagrow.domain.model.CareModule
import com.aquagrow.ui.components.AppCard
import com.aquagrow.ui.components.AppFilterChip
import com.aquagrow.ui.components.AppPhoto
import com.aquagrow.ui.components.AppTopBar
import com.aquagrow.ui.components.PhotoSourceSheet
import com.aquagrow.ui.components.PrimaryButton
import com.aquagrow.ui.components.SecondaryButton
import com.aquagrow.ui.components.SwitchRow
import com.aquagrow.ui.theme.AquaGrowTheme
import com.aquagrow.ui.theme.Dimens
import com.aquagrow.ui.theme.ModuleTheme
import kotlinx.coroutines.delay

private val GARDEN_STARTERS = listOf(
    R.string.assistant_starter_garden_1,
    R.string.assistant_starter_garden_2,
    R.string.assistant_starter_garden_3,
    R.string.assistant_starter_garden_4,
)
private val AQUARIUM_STARTERS = listOf(
    R.string.assistant_starter_aquarium_1,
    R.string.assistant_starter_aquarium_2,
    R.string.assistant_starter_aquarium_3,
    R.string.assistant_starter_aquarium_4,
)

@Composable
fun AssistantScreen(onBack: () -> Unit, onOpenProfile: () -> Unit, vm: AssistantViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    ModuleTheme(s.module) {
        AssistantContent(
            s = s,
            onBack = onBack,
            onOpenProfile = onOpenProfile,
            onSend = vm::send,
            onRetry = vm::retry,
            onSelectSubject = vm::selectSubject,
            onIncludeLogs = vm::setIncludeLogs,
            onPhoto = vm::attachPhoto,
        )
    }
}

@Composable
private fun AssistantContent(
    s: AssistantUiState,
    onBack: () -> Unit,
    onOpenProfile: () -> Unit,
    onSend: (String) -> Unit,
    onRetry: () -> Unit,
    onSelectSubject: (Long?) -> Unit,
    onIncludeLogs: (Boolean) -> Unit,
    onPhoto: (String?) -> Unit,
) {
    val garden = s.module == CareModule.GARDEN
    val title = stringResource(if (garden) R.string.assistant_garden_title else R.string.assistant_aquarium_title)
    var input by rememberSaveable { mutableStateOf("") }
    var photoSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val usable = s.enabled && !s.notConfigured

    // Keep the newest message in view.
    LaunchedEffect(s.items.size, s.loading, s.problem) {
        val last = listState.layoutInfo.totalItemsCount - 1
        if (last >= 0 && s.items.isNotEmpty()) listState.animateScrollToItem(last)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title, onBack) },
        bottomBar = {
            if (usable) {
                InputBar(
                    text = input,
                    onText = { input = it.take(2_000) },
                    photoPath = s.photoPath,
                    sendEnabled = s.canSend && input.isNotBlank(),
                    onSend = {
                        onSend(input)
                        input = ""
                    },
                    onAttach = { photoSheet = true },
                    onRemovePhoto = { onPhoto(null) },
                )
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                !s.enabled -> item(key = "off") {
                    NoticeCard(R.drawable.ic_info, stringResource(R.string.assistant_disabled_title), stringResource(R.string.assistant_disabled_body)) {
                        PrimaryButton(stringResource(R.string.assistant_open_profile), onOpenProfile, Modifier.padding(top = 12.dp))
                    }
                }

                s.notConfigured -> item(key = "nocfg") {
                    NoticeCard(R.drawable.ic_info, stringResource(R.string.assistant_not_configured_title), stringResource(R.string.assistant_not_configured_body))
                }

                else -> {
                    item(key = "intro") {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                stringResource(if (garden) R.string.assistant_intro_garden else R.string.assistant_intro_aquarium),
                                style = MaterialTheme.typography.bodyMedium,
                                color = AquaGrowTheme.colors.mutedText,
                            )
                            Text(stringResource(R.string.assistant_privacy_note), style = MaterialTheme.typography.bodySmall, color = AquaGrowTheme.colors.mutedText)
                        }
                    }
                    item(key = "context") { ContextCard(s, onSelectSubject, onIncludeLogs) }
                    if (s.items.isEmpty()) {
                        item(key = "starters-h") {
                            Text(stringResource(R.string.assistant_try_asking), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
                        }
                        items(if (garden) GARDEN_STARTERS else AQUARIUM_STARTERS, key = { it }) { res ->
                            val q = stringResource(res)
                            AppCard(Modifier.fillMaxWidth(), onClick = if (s.canSend) ({ onSend(q) }) else null, contentPadding = 14.dp) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(painterResource(R.drawable.ic_forum), null, tint = AquaGrowTheme.module.primary)
                                    Text(q, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }
                    itemsIndexed(s.items, key = { i, _ -> "m$i" }) { i, item ->
                        val newest = i == s.items.lastIndex
                        when (item) {
                            is ChatItem.Question -> QuestionBubble(item)
                            is ChatItem.Answer -> AnswerBubble(item, newest)
                            is ChatItem.OfflineTips -> OfflineCard(item, newest)
                        }
                    }
                    if (s.loading) {
                        item(key = "loading") {
                            Row(
                                Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                                Text(stringResource(R.string.assistant_thinking), color = AquaGrowTheme.colors.mutedText)
                            }
                        }
                    }
                    s.problem?.let { p -> item(key = "problem") { ProblemCard(p, onRetry) } }
                }
            }
        }
    }

    if (photoSheet) {
        PhotoSourceSheet(onPicked = {
            onPhoto(it)
            photoSheet = false
        }, onDismiss = { photoSheet = false })
    }
}

@Composable
private fun ContextCard(s: AssistantUiState, onSelect: (Long?) -> Unit, onIncludeLogs: (Boolean) -> Unit) {
    if (s.subjects.isEmpty()) return
    val garden = s.module == CareModule.GARDEN
    AppCard(Modifier.fillMaxWidth(), contentPadding = 16.dp) {
        Text(stringResource(R.string.assistant_context_title), style = MaterialTheme.typography.titleSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            item { AppFilterChip(s.selectedSubject == null, stringResource(R.string.assistant_context_none), { onSelect(null) }) }
            items(s.subjects, key = { it.id }) { o -> AppFilterChip(s.selectedSubject == o.id, o.name, { onSelect(o.id) }) }
        }
        if (s.selectedSubject != null) {
            SwitchRow(
                stringResource(R.string.assistant_context_logs),
                s.includeLogs,
                onIncludeLogs,
                supporting = stringResource(if (garden) R.string.assistant_context_logs_body_garden else R.string.assistant_context_logs_body_aquarium),
            )
        }
        Text(stringResource(R.string.assistant_context_hint), style = MaterialTheme.typography.bodySmall, color = AquaGrowTheme.colors.mutedText, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun QuestionBubble(item: ChatItem.Question) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Surface(
            shape = RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp),
            color = AquaGrowTheme.module.container,
            modifier = Modifier.widthIn(max = 320.dp).semantics(mergeDescendants = true) {},
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item.photoPath?.let {
                    AppPhoto(it, R.drawable.ic_image, AquaGrowTheme.module, Modifier.size(160.dp), contentDescription = stringResource(R.string.assistant_photo_attached))
                }
                Text(item.text, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun AnswerBubble(item: ChatItem.Answer, newest: Boolean) {
    Surface(
        shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, AquaGrowTheme.colors.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { if (newest) liveRegion = LiveRegionMode.Polite },
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.assistant_name), style = MaterialTheme.typography.labelLarge, color = AquaGrowTheme.module.primary)
            Text(item.text, style = MaterialTheme.typography.bodyLarge)
            if (item.truncated) Text(stringResource(R.string.assistant_answer_cut), style = MaterialTheme.typography.bodySmall, color = AquaGrowTheme.colors.mutedText)
            Disclaimer()
        }
    }
}

@Composable
private fun OfflineCard(item: ChatItem.OfflineTips, newest: Boolean) {
    NoticeCard(
        R.drawable.ic_wifi_off,
        stringResource(R.string.assistant_offline_title),
        stringResource(R.string.assistant_offline_body),
        Modifier.semantics(mergeDescendants = true) { if (newest) liveRegion = LiveRegionMode.Polite },
    ) {
        item.tips.forEach { tip ->
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("•", style = MaterialTheme.typography.bodyMedium)
                Text(tip, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Disclaimer(Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun Disclaimer(modifier: Modifier = Modifier) {
    Column(modifier) {
        HorizontalDivider(color = AquaGrowTheme.colors.cardBorder)
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_info), null, Modifier.size(16.dp), tint = AquaGrowTheme.colors.mutedText)
            Text(stringResource(R.string.assistant_disclaimer), style = MaterialTheme.typography.bodySmall, color = AquaGrowTheme.colors.mutedText)
        }
    }
}

@Composable
private fun ProblemCard(p: AssistantProblem, onRetry: () -> Unit) {
    var secondsLeft by remember(p) { mutableIntStateOf((p as? AssistantProblem.RateLimited)?.retryAfterSeconds ?: 0) }
    LaunchedEffect(p) {
        while (secondsLeft > 0) {
            delay(1_000)
            secondsLeft--
        }
    }
    val text = when (p) {
        is AssistantProblem.RateLimited -> if (secondsLeft > 0) stringResource(R.string.assistant_rate_limited, secondsLeft) else stringResource(R.string.assistant_rate_limited_ready)
        is AssistantProblem.Failed -> stringResource(if (p.retryable) R.string.assistant_failed else R.string.assistant_failed_permanent)
        AssistantProblem.Declined -> stringResource(R.string.assistant_declined)
    }
    val canRetry = when (p) {
        is AssistantProblem.RateLimited -> secondsLeft == 0
        is AssistantProblem.Failed -> p.retryable
        AssistantProblem.Declined -> false
    }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = AquaGrowTheme.colors.warningContainer,
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(painterResource(R.drawable.ic_warning), null, tint = AquaGrowTheme.colors.onWarningContainer)
                Text(text, style = MaterialTheme.typography.bodyMedium, color = AquaGrowTheme.colors.onWarningContainer)
            }
            if (p !is AssistantProblem.Declined) {
                SecondaryButton(stringResource(R.string.assistant_retry), onRetry, Modifier.padding(top = 12.dp), enabled = canRetry, icon = R.drawable.ic_restart_alt)
            }
        }
    }
}

@Composable
private fun NoticeCard(icon: Int, title: String, body: String, modifier: Modifier = Modifier, extra: @Composable () -> Unit = {}) {
    AppCard(modifier.fillMaxWidth(), contentPadding = 20.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(painterResource(icon), null, tint = AquaGrowTheme.module.primary)
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                Text(body, style = MaterialTheme.typography.bodyMedium, color = AquaGrowTheme.colors.mutedText, modifier = Modifier.padding(top = 4.dp))
                extra()
            }
        }
    }
}

@Composable
private fun InputBar(
    text: String,
    onText: (String) -> Unit,
    photoPath: String?,
    sendEnabled: Boolean,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    onRemovePhoto: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
        Column(Modifier.navigationBarsPadding().imePadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
            if (photoPath != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    AppPhoto(photoPath, R.drawable.ic_image, AquaGrowTheme.module, Modifier.size(48.dp), contentDescription = stringResource(R.string.assistant_photo_attached))
                    Text(stringResource(R.string.assistant_photo_attached), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = onRemovePhoto) { Icon(painterResource(R.drawable.ic_close), stringResource(R.string.assistant_remove_photo)) }
                }
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onAttach, modifier = Modifier.size(56.dp)) {
                    Icon(painterResource(R.drawable.ic_add_photo_alternate), stringResource(R.string.assistant_attach_photo))
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = onText,
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                    placeholder = { Text(stringResource(R.string.assistant_input_label)) },
                    shape = RoundedCornerShape(28.dp),
                    maxLines = 4,
                )
                FilledIconButton(
                    onClick = onSend,
                    enabled = sendEnabled,
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = AquaGrowTheme.module.primary, contentColor = AquaGrowTheme.module.onPrimary),
                ) {
                    Icon(painterResource(R.drawable.ic_send), stringResource(R.string.assistant_send))
                }
            }
        }
    }
}
