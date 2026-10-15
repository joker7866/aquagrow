package com.aquagrow.data.local.db

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConvertersTest {
    private val c = Converters()

    @Test
    fun localDate_roundTrips() {
        val date = LocalDate.of(2026, 2, 28)
        assertEquals(date, c.longToLocalDate(c.localDateToLong(date)))
    }

    @Test
    fun localTime_roundTrips() {
        val time = LocalTime.of(22, 30)
        assertEquals(time, c.intToLocalTime(c.localTimeToInt(time)))
    }

    @Test
    fun instant_roundTripsToMillis() {
        val instant = Instant.ofEpochMilli(1_780_000_000_123)
        assertEquals(instant, c.longToInstant(c.instantToLong(instant)))
    }

    @Test
    fun stringList_roundTripsIncludingCommasAndQuotes() {
        val stages = listOf("Seed", "Flowering, early", "\"Fruit\" set")
        assertEquals(stages, c.jsonToStringList(c.stringListToJson(stages)))
    }

    @Test
    fun nulls_stayNull() {
        assertNull(c.localDateToLong(null))
        assertNull(c.longToInstant(null))
        assertNull(c.jsonToStringList(null))
    }
}
