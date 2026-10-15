package com.aquagrow.ui.screens.tools

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aquagrow.R
import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.model.Photo
import com.aquagrow.domain.repository.AquariumRepository
import com.aquagrow.domain.repository.BackupPreview
import com.aquagrow.domain.repository.BackupRepository
import com.aquagrow.domain.repository.GardenRepository
import com.aquagrow.domain.repository.PhotoRepository
import com.aquagrow.domain.repository.RestoreMode
import com.aquagrow.domain.repository.SearchRepository
import com.aquagrow.domain.repository.SearchResults
import com.aquagrow.ui.components.AppCard
import com.aquagrow.ui.components.AppFilterChip
import com.aquagrow.ui.components.AppPhoto
import com.aquagrow.ui.components.AppTextField
import com.aquagrow.ui.components.AppTopBar
import com.aquagrow.ui.components.IconTile
import com.aquagrow.ui.components.PrimaryButton
import com.aquagrow.ui.components.SecondaryButton
import com.aquagrow.ui.components.SectionHeader
import com.aquagrow.ui.components.SwitchRow
import com.aquagrow.ui.components.displayTitle
import com.aquagrow.ui.components.rememberFormatters
import com.aquagrow.ui.components.taskIcon
import com.aquagrow.ui.navigation.BackupRoute
import com.aquagrow.ui.theme.AquaGrowTheme
import com.aquagrow.ui.theme.Dimens
import com.aquagrow.ui.theme.forModule
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ---------------- Search ----------------

@HiltViewModel
class SearchViewModel @Inject constructor(repo: SearchRepository) : ViewModel() {
    val query = MutableStateFlow("")

    @OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val results: StateFlow<SearchResults?> = query.debounce(250).mapLatest { q -> if (q.trim().length < 2) null else repo.search(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

data class SearchActions(
    val onPlant: (Long) -> Unit = {},
    val onTank: (Long) -> Unit = {},
    val onFish: (Long) -> Unit = {},
    val onTask: (Long) -> Unit = {},
)

@Composable
fun SearchScreen(onBack: () -> Unit, actions: SearchActions, vm: SearchViewModel = hiltViewModel()) {
    val q by vm.query.collectAsStateWithLifecycle()
    val r by vm.results.collectAsStateWithLifecycle()
    val ext = AquaGrowTheme.colors
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = { AppTopBar(stringResource(R.string.search_title), onBack) }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(Dimens.ScreenPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { AppTextField(q, { vm.query.value = it }, stringResource(R.string.search_hint), maxLength = 60) }
            val res = r
            when {
                q.trim().length < 2 -> item { Text(stringResource(R.string.search_start), color = ext.mutedText) }

                res == null -> item { CircularProgressIndicator() }

                res.isEmpty -> item { Text(stringResource(R.string.search_none, q.trim()), color = ext.mutedText) }

                else -> {
                    fun header(id: Int) = item(key = "h$id") { SectionHeader(stringResource(id)) }
                    if (res.plants.isNotEmpty()) header(R.string.search_plants)
                    items(res.plants, key = { "p${it.id}" }) { p -> ResultRow(R.drawable.ic_sprout, CareModule.GARDEN, listOfNotNull(p.name, p.variety).joinToString(" · "), p.location ?: p.notes) { actions.onPlant(p.id) } }
                    if (res.tanks.isNotEmpty()) header(R.string.search_tanks)
                    items(res.tanks, key = { "a${it.id}" }) { a -> ResultRow(R.drawable.ic_web_asset, CareModule.AQUARIUM, a.name, a.location ?: a.notes) { actions.onTank(a.id) } }
                    if (res.fish.isNotEmpty()) header(R.string.search_fish)
                    items(res.fish, key = { "f${it.id}" }) { f -> ResultRow(R.drawable.ic_fish, CareModule.AQUARIUM, f.displayName, "× ${f.count}") { actions.onFish(f.id) } }
                    if (res.tasks.isNotEmpty()) header(R.string.search_tasks)
                    items(res.tasks, key = { "t${it.id}" }) { t -> ResultRow(taskIcon(t.type, t.maintenanceType), t.module, t.displayTitle(), t.notes) { actions.onTask(t.id) } }
                    if (res.notes.isNotEmpty()) header(R.string.search_notes)
                    items(res.notes, key = { "n${it.id}" }) { n -> ResultRow(R.drawable.ic_eco, CareModule.GARDEN, n.note ?: "", n.stageName) { actions.onPlant(n.plantId) } }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(icon: Int, module: CareModule, title: String, subtitle: String?, onClick: () -> Unit) {
    AppCard(Modifier.fillMaxWidth(), onClick = onClick, contentPadding = 12.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon, AquaGrowTheme.colors.forModule(module), size = 40.dp)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = AquaGrowTheme.colors.mutedText, maxLines = 2) }
            }
        }
    }
}

// ---------------- Gallery ----------------

enum class GalleryFilter { ALL, PLANTS, FISH, TANKS }

data class GalleryUiState(
    val filter: GalleryFilter = GalleryFilter.ALL,
    val subject: Long? = null,
    val photos: List<Photo> = emptyList(),
    val subjects: List<Pair<Long, String>> = emptyList(),
    val names: Map<Photo, String> = emptyMap(),
)

@HiltViewModel
class GalleryViewModel @Inject constructor(photos: PhotoRepository, garden: GardenRepository, aquarium: AquariumRepository) : ViewModel() {
    private val filter = MutableStateFlow(GalleryFilter.ALL)
    private val subject = MutableStateFlow<Long?>(null)

    val state: StateFlow<GalleryUiState> = combine(
        combine(filter, subject, ::Pair),
        photos.observeAll(),
        garden.observePlants(),
        aquarium.observeAquariums(),
        aquarium.observeActiveFish(),
    ) { (f, sub), all, plants, tanks, fish ->
        val plantNames = plants.associate { it.id to it.name }
        val tankNames = tanks.associate { it.id to it.name }
        val fishNames = fish.associate { it.id to it.displayName }
        val filtered = all.filter {
            when (f) {
                GalleryFilter.ALL -> true
                GalleryFilter.PLANTS -> it.plantId != null && (sub == null || it.plantId == sub)
                GalleryFilter.FISH -> it.fishId != null && (sub == null || it.fishId == sub)
                GalleryFilter.TANKS -> it.aquariumId != null && it.fishId == null && (sub == null || it.aquariumId == sub)
            }
        }
        GalleryUiState(
            filter = f,
            subject = sub,
            photos = filtered,
            subjects = when (f) {
                GalleryFilter.PLANTS -> plantNames.toList()
                GalleryFilter.FISH -> fishNames.toList()
                GalleryFilter.TANKS -> tankNames.toList()
                GalleryFilter.ALL -> emptyList()
            },
            names = filtered.associateWith { p -> p.fishId?.let(fishNames::get) ?: p.plantId?.let(plantNames::get) ?: p.aquariumId?.let(tankNames::get) ?: "" },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GalleryUiState())

    fun setFilter(f: GalleryFilter) {
        filter.value = f
        subject.value = null
    }
    fun setSubject(id: Long?) {
        subject.value = id
    }
}

@Composable
fun GalleryScreen(onBack: () -> Unit, onOpenPlant: (Long) -> Unit, onOpenTank: (Long) -> Unit, vm: GalleryViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val f = rememberFormatters()
    val zone = ZoneId.systemDefault()
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = { AppTopBar(stringResource(R.string.gallery_title), onBack) }) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = Dimens.ScreenPadding)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(GalleryFilter.entries) { g ->
                    AppFilterChip(
                        s.filter == g,
                        stringResource(
                            when (g) {
                                GalleryFilter.ALL -> R.string.gallery_all
                                GalleryFilter.PLANTS -> R.string.gallery_plants
                                GalleryFilter.FISH -> R.string.gallery_fish
                                GalleryFilter.TANKS -> R.string.gallery_tanks
                            },
                        ),
                        { vm.setFilter(g) },
                        selectedColor = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            if (s.subjects.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    items(s.subjects, key = { it.first }) { (id, name) -> AppFilterChip(s.subject == id, name, { vm.setSubject(if (s.subject == id) null else id) }) }
                }
            }
            if (s.photos.isEmpty()) Text(stringResource(R.string.gallery_empty), color = AquaGrowTheme.colors.mutedText, modifier = Modifier.padding(vertical = 16.dp))
            LazyVerticalGrid(GridCells.Adaptive(110.dp), Modifier.fillMaxSize().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(s.photos, key = { it.id }) { p ->
                    Column(Modifier.clickable { p.plantId?.let(onOpenPlant) ?: p.aquariumId?.let(onOpenTank) }) {
                        val label = "${s.names[p].orEmpty()} · ${f.dayMonth.format(p.takenAt.atZone(zone))}"
                        AppPhoto(p.filePath, R.drawable.ic_image, if (p.plantId != null) AquaGrowTheme.colors.garden else AquaGrowTheme.colors.aquarium, Modifier.fillMaxWidth().aspectRatio(1f), contentDescription = label)
                        Text(label, style = MaterialTheme.typography.labelSmall, color = AquaGrowTheme.colors.mutedText, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
    }
}

// ---------------- Backup ----------------

/** Outcome of the last backup operation; formatted into text by the screen. */
sealed interface BackupResult {
    data class Exported(val counts: Map<String, Int>) : BackupResult
    data class Restored(val counts: Map<String, Int>) : BackupResult
    data class IcsExported(val events: Int) : BackupResult
    data class Failed(val reason: String) : BackupResult
}

data class BackupUiState(
    val action: String = "backup",
    val busy: Boolean = false,
    val includePhotos: Boolean = false,
    val result: BackupResult? = null,
    val pendingRestore: Pair<String, BackupPreview>? = null,
)

@HiltViewModel
class BackupViewModel @Inject constructor(savedStateHandle: SavedStateHandle, private val repo: BackupRepository) : ViewModel() {
    val state = MutableStateFlow(BackupUiState(action = savedStateHandle.toRoute<BackupRoute>().action))

    private fun run(block: suspend () -> BackupResult) {
        state.value = state.value.copy(busy = true, result = null)
        viewModelScope.launch {
            val r = try {
                block()
            } catch (e: Exception) {
                BackupResult.Failed(e.message ?: e.javaClass.simpleName)
            }
            state.value = state.value.copy(busy = false, result = r)
        }
    }

    fun setIncludePhotos(on: Boolean) {
        state.value = state.value.copy(includePhotos = on)
    }
    fun export(uri: String) = run { BackupResult.Exported(repo.export(uri, state.value.includePhotos)) }
    fun exportIcs(uri: String) = run { BackupResult.IcsExported(repo.exportIcs(uri)) }

    fun inspect(uri: String) {
        state.value = state.value.copy(busy = true, result = null)
        viewModelScope.launch {
            try {
                val preview = repo.inspect(uri)
                state.value = state.value.copy(busy = false, pendingRestore = uri to preview)
            } catch (e: Exception) {
                state.value = state.value.copy(busy = false, result = BackupResult.Failed(e.message ?: e.javaClass.simpleName))
            }
        }
    }

    fun restore(mode: RestoreMode) {
        val (uri, _) = state.value.pendingRestore ?: return
        state.value = state.value.copy(pendingRestore = null)
        run { BackupResult.Restored(repo.restore(uri, mode)) }
    }

    fun cancelRestore() {
        state.value = state.value.copy(pendingRestore = null)
    }
}

@Composable
private fun countsText(c: Map<String, Int>): String = stringResource(
    R.string.backup_counts,
    c["plants"] ?: 0,
    c["aquariums"] ?: 0,
    c["fish"] ?: 0,
    c["tasks"] ?: 0,
    c["logs"] ?: 0,
    c["photos"] ?: 0,
)

@Composable
fun BackupScreen(onBack: () -> Unit, onRestored: () -> Unit = {}, vm: BackupViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val f = rememberFormatters()
    val snackbar = remember { SnackbarHostState() }
    val resultText: String? = when (val r = s.result) {
        is BackupResult.Exported -> stringResource(R.string.backup_export_done, countsText(r.counts))
        is BackupResult.Restored -> stringResource(R.string.backup_restore_done, countsText(r.counts))
        is BackupResult.IcsExported -> pluralStringResource(R.plurals.backup_ics_done, r.events, r.events)
        is BackupResult.Failed -> stringResource(R.string.backup_failed, r.reason)
        null -> null
    }

    LaunchedEffect(s.result) {
        if (s.result is BackupResult.Restored) onRestored()
        resultText?.let { snackbar.showSnackbar(it) }
    }

    val createJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let { vm.export(it.toString()) } }
    val createZip = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri -> uri?.let { vm.export(it.toString()) } }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { vm.inspect(it.toString()) } }
    val createIcs = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/calendar")) { uri -> uri?.let { vm.exportIcs(it.toString()) } }
    val stamp = java.time.LocalDate.now().toString()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(stringResource(R.string.backup_title), onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(Dimens.ScreenPadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (s.busy) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.backup_working))
                    }
                }
            }
            item {
                AppCard(Modifier.fillMaxWidth(), contentPadding = 20.dp) {
                    Text(stringResource(R.string.backup_export_title), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.backup_export_body), style = MaterialTheme.typography.bodyMedium, color = AquaGrowTheme.colors.mutedText, modifier = Modifier.padding(vertical = 8.dp))
                    SwitchRow(stringResource(R.string.backup_include_photos), s.includePhotos, vm::setIncludePhotos)
                    PrimaryButton(stringResource(R.string.backup_export_button), {
                        if (s.includePhotos) createZip.launch("aquagrow-backup-$stamp.zip") else createJson.launch("aquagrow-backup-$stamp.json")
                    }, enabled = !s.busy, modifier = Modifier.padding(top = 8.dp))
                }
            }
            item {
                AppCard(Modifier.fillMaxWidth(), contentPadding = 20.dp) {
                    Text(stringResource(R.string.backup_restore_title), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.backup_restore_body), style = MaterialTheme.typography.bodyMedium, color = AquaGrowTheme.colors.mutedText, modifier = Modifier.padding(vertical = 8.dp))
                    SecondaryButton(stringResource(R.string.backup_restore_button), { openBackup.launch(arrayOf("application/json", "application/zip", "application/octet-stream", "text/plain")) }, enabled = !s.busy, emphasized = true)
                }
            }
            item {
                AppCard(Modifier.fillMaxWidth(), contentPadding = 20.dp) {
                    Text(stringResource(R.string.backup_ics_title), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.backup_ics_body), style = MaterialTheme.typography.bodyMedium, color = AquaGrowTheme.colors.mutedText, modifier = Modifier.padding(vertical = 8.dp))
                    SecondaryButton(stringResource(R.string.backup_ics_button), { createIcs.launch("aquagrow-calendar-$stamp.ics") }, enabled = !s.busy, emphasized = true)
                }
            }
        }
    }

    // Auto-open the relevant picker when arriving from Welcome ("Restore from a backup") or Profile shortcuts.
    var autoLaunched by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (autoLaunched) return@LaunchedEffect // not again after rotation / process restore
        autoLaunched = true
        when (s.action) {
            "restore" -> openBackup.launch(arrayOf("application/json", "application/zip", "application/octet-stream", "text/plain"))
            "ics" -> createIcs.launch("aquagrow-calendar-$stamp.ics")
        }
    }

    s.pendingRestore?.let { (_, preview) ->
        val summary = countsText(preview.counts)
        AlertDialog(
            onDismissRequest = vm::cancelRestore,
            title = { Text(stringResource(R.string.backup_preview_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.backup_preview_body, f.dayMonthYear.format(preview.exportedAt.atZone(ZoneId.systemDefault())), preview.appVersion, summary))
                    Box(Modifier.fillMaxWidth().clickable { vm.restore(RestoreMode.MERGE) }.padding(vertical = 8.dp)) {
                        Column {
                            Text(stringResource(R.string.backup_merge), style = MaterialTheme.typography.titleSmall, color = AquaGrowTheme.module.primary)
                            Text(stringResource(R.string.backup_merge_body), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Box(Modifier.fillMaxWidth().clickable { vm.restore(RestoreMode.REPLACE) }.padding(vertical = 8.dp)) {
                        Column {
                            Text(stringResource(R.string.backup_replace), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                            Text(stringResource(R.string.backup_replace_body), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(vm::cancelRestore) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}
