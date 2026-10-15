package com.aquagrow.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aquagrow.R
import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.model.MaintenanceType
import com.aquagrow.domain.model.TaskType
import com.aquagrow.ui.preview.ThemePreviews
import com.aquagrow.ui.theme.AquaGrowTheme
import com.aquagrow.ui.theme.Dimens
import com.aquagrow.ui.theme.ModuleColors
import com.aquagrow.ui.theme.forModule

@DrawableRes
fun taskIcon(type: TaskType, maintenance: MaintenanceType? = null): Int = when (type) {
    TaskType.WATERING -> R.drawable.ic_water_drop

    TaskType.FERTILIZING -> R.drawable.ic_compost

    TaskType.SOWING -> R.drawable.ic_grass

    TaskType.TRANSPLANTING -> R.drawable.ic_sprout

    TaskType.HARVEST -> R.drawable.ic_agriculture

    TaskType.FEEDING -> R.drawable.ic_fish

    TaskType.WATER_TEST -> R.drawable.ic_science

    TaskType.MAINTENANCE -> when (maintenance) {
        MaintenanceType.WATER_CHANGE -> R.drawable.ic_water
        MaintenanceType.FILTER_INSPECTION, MaintenanceType.FILTER_MAINTENANCE -> R.drawable.ic_filter_alt
        MaintenanceType.GLASS_CLEANING, MaintenanceType.GRAVEL_VACUUM -> R.drawable.ic_cleaning_services
        MaintenanceType.PLANT_TRIMMING -> R.drawable.ic_eco
        else -> R.drawable.ic_build
    }

    TaskType.CUSTOM -> R.drawable.ic_task_alt
}

@StringRes
fun taskTypeLabel(type: TaskType): Int = when (type) {
    TaskType.WATERING -> R.string.task_type_watering
    TaskType.FERTILIZING -> R.string.task_type_fertilizing
    TaskType.SOWING -> R.string.task_type_sowing
    TaskType.TRANSPLANTING -> R.string.task_type_transplanting
    TaskType.HARVEST -> R.string.task_type_harvest
    TaskType.FEEDING -> R.string.task_type_feeding
    TaskType.WATER_TEST -> R.string.task_type_water_test
    TaskType.MAINTENANCE -> R.string.task_type_maintenance
    TaskType.CUSTOM -> R.string.task_type_custom
}

/**
 * Icon tile, title, meta line and a round check button (mockup: Home "Today").
 * Check states: done = filled sage with white tick, pending = grey outline with a faint tick,
 * overdue = orange outline with an orange tick. The whole row is one toggle for TalkBack.
 */
@Composable
fun TaskRow(
    @DrawableRes icon: Int,
    title: String,
    meta: String,
    done: Boolean,
    onToggleDone: (Boolean) -> Unit,
    module: CareModule,
    modifier: Modifier = Modifier,
    metaIsWarning: Boolean = false,
) {
    val colors = AquaGrowTheme.colors.forModule(module)
    val contentAlpha = if (done) 0.55f else 1f
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .toggleable(value = done, role = Role.Checkbox, onValueChange = onToggleDone)
            .semantics { contentDescription = "$title, $meta" }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconTile(icon = icon, colors = colors, size = 48.dp, modifier = Modifier.graphicsLayer { alpha = contentAlpha })
        Column(Modifier.weight(1f).graphicsLayer { alpha = contentAlpha }) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = meta,
                style = MaterialTheme.typography.bodyMedium,
                color = if (metaIsWarning && !done) AquaGrowTheme.colors.onWarningContainer else AquaGrowTheme.colors.mutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        CheckCircle(done = done, overdue = metaIsWarning)
    }
}

@Composable
private fun CheckCircle(done: Boolean, overdue: Boolean) {
    val ext = AquaGrowTheme.colors
    val bg by animateColorAsState(if (done) ext.garden.accent else Color.Transparent, label = "check-bg")
    val ring = if (overdue) OverdueOrange else ext.cardBorder
    val tick = when {
        done -> Color.White
        overdue -> OverdueOrange
        else -> ext.mutedText.copy(alpha = 0.35f)
    }
    Surface(
        shape = CircleShape,
        color = bg,
        border = if (done) null else BorderStroke(2.dp, ring),
        modifier = Modifier.size(Dimens.MinTouchTarget),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = tick, modifier = Modifier.size(22.dp))
        }
    }
}

/** The orange used for overdue ticks in the mockup (meets 3:1 for graphics on white). */
private val OverdueOrange = Color(0xFFC8742A)

@ThemePreviews
@Composable
private fun TaskRowPreview() {
    AquaGrowTheme {
        AppCard {
            TaskRow(R.drawable.ic_water_drop, "Water · Tomato", "8:00 AM", false, {}, CareModule.GARDEN)
            HorizontalDivider(color = AquaGrowTheme.colors.cardBorder)
            TaskRow(R.drawable.ic_fish, "Feed · Guppies", "Overdue · was due Mon", false, {}, CareModule.AQUARIUM, metaIsWarning = true)
            HorizontalDivider(color = AquaGrowTheme.colors.cardBorder)
            TaskRow(R.drawable.ic_science, "Test water · Living room tank", "6:00 PM", true, {}, CareModule.AQUARIUM)
        }
    }
}
