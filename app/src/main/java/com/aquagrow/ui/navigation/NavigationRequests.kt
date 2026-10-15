package com.aquagrow.ui.navigation

import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface NavRequest {
    data class OpenPlant(val plantId: Long) : NavRequest
    data class OpenTank(val aquariumId: Long) : NavRequest
    data class OpenCalendar(val date: LocalDate) : NavRequest
    data object OpenInventory : NavRequest
}

/** Hand-off for navigation that starts outside Compose (notification taps). */
@Singleton
class NavigationRequests @Inject constructor() {
    private val _pending = MutableStateFlow<NavRequest?>(null)
    val pending: StateFlow<NavRequest?> = _pending.asStateFlow()

    /** Date the Calendar should select next time it opens. */
    private val _calendarDate = MutableStateFlow<LocalDate?>(null)
    val calendarDate: StateFlow<LocalDate?> = _calendarDate.asStateFlow()

    fun request(r: NavRequest) {
        if (r is NavRequest.OpenCalendar) _calendarDate.value = r.date
        _pending.value = r
    }

    fun consumed() {
        _pending.value = null
    }
    fun calendarDateConsumed() {
        _calendarDate.value = null
    }
}
