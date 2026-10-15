package com.aquagrow.data.local.db

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.serialization.json.Json

/**
 * Time is stored zone-free on purpose: calendar days as [LocalDate] (epoch day) and
 * reminder times as [LocalTime]. Due dates then stay correct when the user changes
 * timezone; the scheduler resolves them against the current zone at fire time.
 * Moments that already happened (log timestamps) are stored as [Instant].
 */
class Converters {
    @TypeConverter
    fun instantToLong(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun longToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun localDateToLong(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter
    fun longToLocalDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun localTimeToInt(value: LocalTime?): Int? = value?.toSecondOfDay()

    @TypeConverter
    fun intToLocalTime(value: Int?): LocalTime? = value?.let { LocalTime.ofSecondOfDay(it.toLong()) }

    @TypeConverter
    fun stringListToJson(value: List<String>?): String? = value?.let { Json.encodeToString(it) }

    @TypeConverter
    fun jsonToStringList(value: String?): List<String>? = value?.let { Json.decodeFromString<List<String>>(it) }
}
