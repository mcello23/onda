package io.github.mcello23.onda.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SleepTimerStatus(
    val active: Boolean = false,
    val remainingMs: Long = 0L,
    val endOfEpisode: Boolean = false,
)

object SleepTimerState {
    private val _status = MutableStateFlow(SleepTimerStatus())
    val status: StateFlow<SleepTimerStatus> = _status.asStateFlow()

    internal fun update(status: SleepTimerStatus) {
        _status.value = status
    }

    internal fun clear() {
        _status.value = SleepTimerStatus()
    }
}
