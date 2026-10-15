package com.aquagrow.domain.time

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

/** Abstracts "now" so date logic can be unit-tested with a fixed clock. */
interface TimeProvider {
    fun now(): Instant
    fun zone(): ZoneId
    fun today(): LocalDate = now().atZone(zone()).toLocalDate()

    /** Emits today's date, then again just after each local midnight. */
    fun observeToday(): Flow<LocalDate> = flow {
        while (true) {
            val today = today()
            emit(today)
            val nextMidnight = today.plusDays(1).atStartOfDay(zone()).toInstant()
            delay(Duration.between(now(), nextMidnight).toMillis().coerceAtLeast(1_000) + 500)
        }
    }.distinctUntilChanged()
}

class SystemTimeProvider : TimeProvider {
    override fun now(): Instant = Instant.now()

    // Read on every call so a timezone change takes effect immediately.
    override fun zone(): ZoneId = ZoneId.systemDefault()
}
