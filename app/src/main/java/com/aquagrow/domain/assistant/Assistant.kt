package com.aquagrow.domain.assistant

import com.aquagrow.domain.catalog.FishCatalog
import com.aquagrow.domain.catalog.PlantCatalog
import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.repository.AquariumRepository
import com.aquagrow.domain.repository.GardenRepository
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.first

enum class ChatRole { USER, ASSISTANT }

data class ChatTurn(val role: ChatRole, val text: String)

/** What the user chose to attach to a question. Nothing is attached unless they pick it. */
sealed interface AssistantSubject {
    data class PlantSubject(val plantId: Long) : AssistantSubject
    data class TankSubject(val aquariumId: Long) : AssistantSubject
}

data class AssistantQuestion(
    val module: CareModule,
    /** Conversation so far, oldest first; the last turn is the new user question. */
    val history: List<ChatTurn>,
    val subject: AssistantSubject? = null,
    val includeRecentLogs: Boolean = false,
    /** Relative photo path under files/, if the user attached a photo. */
    val photoPath: String? = null,
)

sealed interface AssistantReply {
    data class Answer(val text: String, val truncated: Boolean = false) : AssistantReply

    /** No connection: general care tips from the built-in catalog instead. */
    data class Offline(val tips: List<String>) : AssistantReply
    data class RateLimited(val retryAfterSeconds: Int) : AssistantReply
    data object Disabled : AssistantReply
    data object NotConfigured : AssistantReply
    data object Declined : AssistantReply
    data class Failed(val retryable: Boolean) : AssistantReply
}

interface AssistantRepository {
    /** False when this build has no assistant backend URL. */
    val isConfigured: Boolean

    suspend fun ask(question: AssistantQuestion): AssistantReply
}

/** Offline fallback: tips from the catalog for the attached plant or tank, else general ones. */
class OfflineTips @Inject constructor(
    private val garden: GardenRepository,
    private val aquarium: AquariumRepository,
) {
    suspend fun forQuestion(q: AssistantQuestion): List<String> {
        val specific = when (val s = q.subject) {
            is AssistantSubject.PlantSubject -> garden.getPlant(s.plantId)?.let { PlantCatalog.find(it.catalogKey)?.careTips }.orEmpty()

            is AssistantSubject.TankSubject -> aquarium.getFishForAquarium(s.aquariumId)
                .mapNotNull { FishCatalog.find(it.speciesKey) }
                .distinctBy { it.key }
                .flatMap { f -> f.careNotes.take(2).map { "${f.name}: $it" } }

            null -> emptyList()
        }
        return (specific + general(q.module)).distinct().take(6)
    }

    private fun general(module: CareModule): List<String> = when (module) {
        CareModule.GARDEN -> GARDEN_TIPS
        CareModule.AQUARIUM -> AQUARIUM_TIPS
    }

    companion object {
        val GARDEN_TIPS = listOf(
            "Water when the top 2–3 cm of soil feels dry, not on a fixed clock. Pots dry faster in summer and wind.",
            "Use pots with drainage holes so roots never sit in water.",
            "Follow the fertilizer label and never go above it; water the plant before feeding.",
            "Check the undersides of leaves every few days for pests; early action is easiest.",
        )
        val AQUARIUM_TIPS = listOf(
            "Test ammonia, nitrite, nitrate, pH and temperature when fish behave unusually.",
            "Do regular partial water changes with dechlorinated, temperature-matched water.",
            "Feed only what fish finish in a couple of minutes; uneaten food spoils the water.",
            "For sick fish, ask an aquatic vet or an experienced local fish store.",
        )
    }
}

/**
 * Turns the subject the user attached into short plain text for the assistant. Only that
 * plant or tank (and its recent logs, if chosen) is included; no other app data.
 */
class BuildAssistantContextUseCase @Inject constructor(
    private val garden: GardenRepository,
    private val aquarium: AquariumRepository,
) {
    private val day = DateTimeFormatter.ofPattern("d MMM yyyy")

    suspend operator fun invoke(subject: AssistantSubject?, includeLogs: Boolean, zone: ZoneId): String? = when (subject) {
        null -> null
        is AssistantSubject.PlantSubject -> plant(subject.plantId, includeLogs, zone)
        is AssistantSubject.TankSubject -> tank(subject.aquariumId, includeLogs, zone)
    }

    private suspend fun plant(id: Long, includeLogs: Boolean, zone: ZoneId): String? {
        val p = garden.getPlant(id) ?: return null
        return buildString {
            appendLine("Plant: ${listOfNotNull(p.name, p.variety).joinToString(", ")}")
            appendLine("Planted: ${day.format(p.plantingDate)}")
            p.growthStages.getOrNull(p.currentStageIndex)?.let { appendLine("Current stage (my estimate): $it") }
            p.location?.let { appendLine("Location: $it") }
            p.sunlight?.let { appendLine("Sunlight: ${it.name.lowercase().replace('_', ' ')}") }
            p.potSize?.let { appendLine("Pot: $it") }
            p.soilMix?.let { appendLine("Soil: $it") }
            p.condition?.let { appendLine("Condition: ${it.name.lowercase().replace('_', ' ')}") }
            if (includeLogs) {
                val watering = garden.observeWateringLogs(id).first().sortedByDescending { it.loggedAt }.take(5)
                if (watering.isNotEmpty()) {
                    appendLine(
                        "Recent watering: " + watering.joinToString("; ") { w ->
                            day.format(w.loggedAt.atZone(zone)) + (if (w.watered) "" else " (skipped)") + (w.soilMoisture?.let { ", soil ${it.name.lowercase()}" } ?: "")
                        },
                    )
                }
                val fert = garden.observeFertilizerLogs(id).first().sortedByDescending { it.appliedAt }.take(3)
                if (fert.isNotEmpty()) {
                    appendLine(
                        "Recent fertilizer: " + fert.joinToString("; ") { f ->
                            day.format(f.appliedAt.atZone(zone)) + ", " + (f.productName ?: f.type.name.lowercase().replace('_', ' '))
                        },
                    )
                }
                val notes = garden.observeGrowthLogs(id).first().sortedByDescending { it.loggedAt }.mapNotNull { g -> g.note?.let { day.format(g.loggedAt.atZone(zone)) + ": " + it } }.take(3)
                if (notes.isNotEmpty()) appendLine("Recent notes: " + notes.joinToString("; "))
            }
        }.trim().take(MAX_CHARS)
    }

    private suspend fun tank(id: Long, includeLogs: Boolean, zone: ZoneId): String? {
        val a = aquarium.getAquarium(id) ?: return null
        val fish = aquarium.getFishForAquarium(id)
        return buildString {
            appendLine("Tank: ${a.name}, ${a.tankType.name.lowercase().replace('_', ' ')}")
            a.volumeLiters?.let { appendLine("Volume: ${"%.0f".format(it)} L") }
            a.setupDate?.let { appendLine("Set up: ${day.format(it)}") }
            listOfNotNull(a.filterType?.let { "filter $it" }, a.heater?.let { "heater $it" }, a.substrate?.let { "substrate $it" })
                .takeIf { it.isNotEmpty() }?.let { appendLine("Equipment: " + it.joinToString(", ")) }
            if (fish.isNotEmpty()) appendLine("Fish: " + fish.joinToString(", ") { "${it.count} × ${it.speciesName}" })
            if (includeLogs) {
                val tests = aquarium.observeWaterTests(id).first().sortedByDescending { it.testedAt }.take(3)
                tests.forEach { t ->
                    val values = listOfNotNull(
                        t.temperatureC?.let { "temp $it °C" },
                        t.ph?.let { "pH $it" },
                        t.ammoniaPpm?.let { "ammonia $it ppm" },
                        t.nitritePpm?.let { "nitrite $it ppm" },
                        t.nitratePpm?.let { "nitrate $it ppm" },
                    )
                    if (values.isNotEmpty()) appendLine("Water test ${day.format(t.testedAt.atZone(zone))}: " + values.joinToString(", "))
                }
                if (tests.isEmpty()) appendLine("No water tests logged.")
                val maint = aquarium.observeMaintenanceLogs(id).first().sortedByDescending { it.performedAt }.take(3)
                if (maint.isNotEmpty()) {
                    appendLine(
                        "Recent maintenance: " + maint.joinToString("; ") { m ->
                            day.format(m.performedAt.atZone(zone)) + ", " + m.type.name.lowercase().replace('_', ' ') + (m.waterChangePercent?.let { " $it%" } ?: "")
                        },
                    )
                }
            }
        }.trim().take(MAX_CHARS)
    }

    companion object {
        const val MAX_CHARS = 3_500
    }
}
