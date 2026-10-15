package com.aquagrow.ui.screens.addaquarium

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.aquagrow.domain.aquarium.TankVolume
import com.aquagrow.domain.model.Aquarium
import com.aquagrow.domain.model.TankType
import com.aquagrow.domain.repository.AquariumRepository
import com.aquagrow.domain.usecase.SaveAquariumUseCase
import com.aquagrow.ui.components.formatReading
import com.aquagrow.ui.navigation.AddAquariumRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TankField { NAME, LENGTH, WIDTH, HEIGHT, VOLUME }

data class TankForm(
    val name: String = "",
    val length: String = "",
    val width: String = "",
    val height: String = "",
    val volume: String = "",
    val volumeManual: Boolean = false,
    val setupDate: LocalDate? = null,
    val tankType: TankType = TankType.COMMUNITY,
    val filter: String = "",
    val heater: String = "",
    val lighting: String = "",
    val substrate: String = "",
    val livePlants: Boolean = false,
    val livePlantsNote: String = "",
    val equipment: String = "",
    val location: String = "",
    val notes: String = "",
    val photoPath: String? = null,
) {
    /** Litres from the typed dimensions, or null if incomplete/invalid. */
    val calculatedVolume: Double? get() = TankVolume.liters(length.toDoubleOrNull(), width.toDoubleOrNull(), height.toDoubleOrNull())

    fun errors(): Set<TankField> = buildSet {
        if (name.isBlank()) add(TankField.NAME)
        fun bad(s: String) = s.isNotBlank() && (s.toDoubleOrNull()?.let { it <= 0 || it > 1000 } ?: true)
        if (bad(length)) add(TankField.LENGTH)
        if (bad(width)) add(TankField.WIDTH)
        if (bad(height)) add(TankField.HEIGHT)
        if (volume.isNotBlank() && (volume.toDoubleOrNull()?.let { it <= 0 || it > 100_000 } ?: true)) add(TankField.VOLUME)
    }
}

data class AddAquariumUiState(
    val isEdit: Boolean,
    val loading: Boolean,
    val form: TankForm = TankForm(),
    val showErrors: Boolean = false,
    val dirty: Boolean = false,
    val saving: Boolean = false,
    val savedId: Long? = null,
    val moreExpanded: Boolean = false,
)

@HiltViewModel
class AddAquariumViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: AquariumRepository,
    private val save: SaveAquariumUseCase,
) : ViewModel() {
    private val id = savedStateHandle.toRoute<AddAquariumRoute>().aquariumId
    private var original: Aquarium? = null
    private val _state = MutableStateFlow(AddAquariumUiState(isEdit = id != null, loading = id != null))
    val state: StateFlow<AddAquariumUiState> = _state.asStateFlow()

    /**
     * New tanks start with the next free default name ("Tank 1", "Tank 2", …), which the user can
     * change. [format] turns a number into the localized name.
     */
    fun suggestName(format: (Int) -> String) {
        if (id != null) return
        viewModelScope.launch {
            val taken = repo.observeAquariums().first().map { it.name.trim().lowercase() }.toSet()
            var n = taken.size + 1
            while (format(n).lowercase() in taken) n++
            _state.update { s -> if (s.form.name.isBlank() && !s.dirty) s.copy(form = s.form.copy(name = format(n))) else s }
        }
    }

    init {
        if (id != null) {
            viewModelScope.launch {
                val a = repo.getAquarium(id)
                original = a
                if (a == null) {
                    _state.update { it.copy(loading = false) }
                    return@launch
                }
                fun d(v: Double?) = v?.let(::formatReading).orEmpty()
                _state.update {
                    it.copy(
                        loading = false,
                        moreExpanded = true,
                        form = TankForm(
                            name = a.name, length = d(a.lengthCm), width = d(a.widthCm), height = d(a.heightCm),
                            volume = d(a.volumeLiters), volumeManual = a.volumeIsManual, setupDate = a.setupDate,
                            tankType = a.tankType, filter = a.filterType.orEmpty(), heater = a.heater.orEmpty(),
                            lighting = a.lighting.orEmpty(), substrate = a.substrate.orEmpty(), livePlants = a.hasLivePlants,
                            livePlantsNote = a.livePlantsNote.orEmpty(), equipment = a.otherEquipment.orEmpty(),
                            location = a.location.orEmpty(), notes = a.notes.orEmpty(), photoPath = a.coverPhotoPath,
                        ),
                    )
                }
            }
        }
    }

    fun update(transform: (TankForm) -> TankForm) = _state.update { s ->
        var f = transform(s.form)
        // Keep the volume in sync with dimensions until the user overrides it.
        if (!f.volumeManual) f = f.copy(volume = f.calculatedVolume?.let(::formatReading).orEmpty())
        s.copy(form = f, dirty = true)
    }

    fun onVolumeTyped(v: String) = update { it.copy(volume = v, volumeManual = true) }
    fun resetVolume() = update { it.copy(volumeManual = false) }
    fun toggleMore() = _state.update { it.copy(moreExpanded = !it.moreExpanded) }

    fun save() {
        val s = _state.value
        if (s.saving) return
        if (s.form.errors().isNotEmpty()) {
            _state.update { it.copy(showErrors = true) }
            return
        }
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val f = s.form
            fun t(v: String) = v.trim().ifEmpty { null }
            val base = original ?: Aquarium(name = "")
            val newId = save(
                base.copy(
                    id = id ?: 0,
                    name = f.name,
                    lengthCm = f.length.toDoubleOrNull(), widthCm = f.width.toDoubleOrNull(), heightCm = f.height.toDoubleOrNull(),
                    volumeLiters = f.volume.toDoubleOrNull(), volumeIsManual = f.volumeManual,
                    tankType = f.tankType, setupDate = f.setupDate, location = t(f.location),
                    filterType = t(f.filter), heater = t(f.heater), lighting = t(f.lighting), substrate = t(f.substrate),
                    hasLivePlants = f.livePlants, livePlantsNote = t(f.livePlantsNote).takeIf { f.livePlants },
                    otherEquipment = t(f.equipment), coverPhotoPath = f.photoPath, notes = t(f.notes),
                ),
            )
            _state.update { it.copy(saving = false, savedId = newId, dirty = false) }
        }
    }
}
