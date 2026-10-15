package com.aquagrow.ui.screens.addfish

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aquagrow.domain.aquarium.CompatibilityChecker
import com.aquagrow.domain.aquarium.CompatibilityWarning
import com.aquagrow.domain.catalog.CatalogFish
import com.aquagrow.domain.catalog.FishCatalog
import com.aquagrow.domain.model.Aquarium
import com.aquagrow.domain.model.Fish
import com.aquagrow.domain.repository.AquariumRepository
import com.aquagrow.domain.time.TimeProvider
import com.aquagrow.domain.usecase.SaveFishUseCase
import com.aquagrow.ui.components.formatReading
import com.aquagrow.ui.navigation.AddFishRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FishField { SPECIES, TANK, COUNT, SIZE }

data class FishForm(
    val speciesKey: String? = null,
    val customSpecies: String = "",
    val nickname: String = "",
    val aquariumId: Long? = null,
    val count: Int = 1,
    val addedDate: LocalDate? = null,
    val age: String = "",
    val sizeText: String = "",
    val feeding: String = "",
    val compatNotes: String = "",
    val health: String = "",
    val careNotes: String = "",
    val photoPath: String? = null,
) {
    val catalog: CatalogFish? get() = FishCatalog.find(speciesKey)
    val speciesName: String get() = catalog?.name ?: customSpecies.trim()

    fun errors(): Set<FishField> = buildSet {
        if (speciesName.isBlank()) add(FishField.SPECIES)
        if (aquariumId == null) add(FishField.TANK)
        if (count < 1) add(FishField.COUNT)
        if (sizeText.isNotBlank() && (sizeText.toDoubleOrNull()?.let { it <= 0 || it > 200 } ?: true)) add(FishField.SIZE)
    }
}

data class AddFishUiState(
    val isEdit: Boolean = false,
    val loading: Boolean = true,
    val tanks: List<Aquarium> = emptyList(),
    val form: FishForm = FishForm(),
    val warnings: List<CompatibilityWarning> = emptyList(),
    val showErrors: Boolean = false,
    val dirty: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val moreExpanded: Boolean = false,
)

@HiltViewModel
class AddFishViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: AquariumRepository,
    private val saveFish: SaveFishUseCase,
    time: TimeProvider,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<AddFishRoute>()
    private val fishId = route.fishId
    private val form = MutableStateFlow(FishForm(aquariumId = route.aquariumId, addedDate = time.today()))
    private val meta = MutableStateFlow(AddFishUiState(isEdit = fishId != null))

    init {
        viewModelScope.launch {
            val existing = fishId?.let { repo.getFish(it) }
            if (existing != null) {
                form.value = FishForm(
                    speciesKey = existing.speciesKey,
                    customSpecies = if (existing.speciesKey == null) existing.speciesName else "",
                    nickname = existing.nickname.orEmpty(), aquariumId = existing.aquariumId, count = existing.count,
                    addedDate = existing.addedDate, age = existing.ageText.orEmpty(), sizeText = existing.sizeCm?.let(::formatReading).orEmpty(),
                    feeding = existing.feedingNeeds.orEmpty(), compatNotes = existing.compatibilityNotes.orEmpty(),
                    health = existing.healthObservations.orEmpty(), careNotes = existing.careNotes.orEmpty(), photoPath = existing.photoPath,
                )
                meta.update { it.copy(moreExpanded = true) }
            }
        }
    }

    private val warnings = form.flatMapLatest { f ->
        val species = f.catalog
        val tankId = f.aquariumId
        if (species == null || tankId == null) {
            flowOf(emptyList())
        } else {
            combine(repo.observeFish(tankId), repo.observeAquarium(tankId)) { fish, tank ->
                CompatibilityChecker.check(species, f.count, fish.filter { it.id != fishId }, tank?.volumeLiters, tank?.ranges ?: com.aquagrow.domain.model.WaterRanges())
            }
        }
    }

    val state: StateFlow<AddFishUiState> = combine(meta, form, repo.observeAquariums(), warnings) { m, f, tanks, w ->
        // Pre-select the only tank, if there is just one.
        val selected = f.aquariumId ?: tanks.singleOrNull()?.id
        m.copy(loading = false, tanks = tanks, form = f.copy(aquariumId = selected), warnings = w)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddFishUiState(isEdit = fishId != null))

    fun update(transform: (FishForm) -> FishForm) {
        form.update { transform(it.copy(aquariumId = state.value.form.aquariumId)) }
        meta.update { it.copy(dirty = true) }
    }

    fun pickSpecies(key: String?) = update { f ->
        val entry = FishCatalog.find(key)
        f.copy(
            speciesKey = key,
            // Pre-fill feeding needs from the catalog only if the user has not written their own.
            feeding = if (f.feeding.isBlank() || f.feeding == f.catalog?.diet) entry?.diet.orEmpty() else f.feeding,
        )
    }

    fun toggleMore() = meta.update { it.copy(moreExpanded = !it.moreExpanded) }

    fun save() {
        val s = state.value
        val f = s.form
        if (s.saving) return
        if (f.errors().isNotEmpty()) {
            meta.update { it.copy(showErrors = true) }
            return
        }
        meta.update { it.copy(saving = true) }
        viewModelScope.launch {
            fun t(v: String) = v.trim().ifEmpty { null }
            saveFish(
                Fish(
                    id = fishId ?: 0, aquariumId = f.aquariumId!!, speciesKey = f.speciesKey, speciesName = f.speciesName,
                    nickname = t(f.nickname), count = f.count, addedDate = f.addedDate, ageText = t(f.age),
                    sizeCm = f.sizeText.toDoubleOrNull(), photoPath = f.photoPath, feedingNeeds = t(f.feeding),
                    compatibilityNotes = t(f.compatNotes), healthObservations = t(f.health), careNotes = t(f.careNotes),
                ),
            )
            meta.update { it.copy(saving = false, saved = true, dirty = false) }
        }
    }

    fun delete() {
        val id = fishId ?: return
        viewModelScope.launch {
            repo.deleteFish(id)
            meta.update { it.copy(saved = true, dirty = false) }
        }
    }
}
