package com.aquagrow.data.local.entity

import androidx.room.Embedded

/** Row for tank cards: latest water test (may be null) plus total fish count. */
data class AquariumSummaryRow(
    @Embedded val aquarium: AquariumEntity,
    @Embedded(prefix = "latest_") val latestTest: WaterQualityLogEntity?,
    val fishCount: Int,
)
