package com.melodify.shared.presentation

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class SleepOption(val label: String, val minutes: Int? = null) {
    OFF("Off"),
    MIN_15("15 Minutes", 15),
    MIN_30("30 Minutes", 30),
    MIN_45("45 Minutes", 45),
    MIN_60("60 Minutes", 60),
    END_OF_TRACK("End of Track")
}

class SleepTimerManager(
    private val scope: CoroutineScope,
    private val onTimerComplete: () -> Unit,
    private val isPlaying: () -> Boolean
) {
    private val _sleepOption = MutableStateFlow(SleepOption.OFF)
    val sleepOption: StateFlow<SleepOption> = _sleepOption.asStateFlow()

    private val _sleepRemainingMs = MutableStateFlow<Long?>(null)
    val sleepRemainingMs: StateFlow<Long?> = _sleepRemainingMs.asStateFlow()

    private var timerJob: Job? = null

    fun setSleepTimer(option: SleepOption) {
        _sleepOption.value = option
        timerJob?.cancel()
        _sleepRemainingMs.value = null

        if (option == SleepOption.OFF || option == SleepOption.END_OF_TRACK) {
            return
        }

        val minutes = option.minutes ?: return
        val totalMs = minutes * 60 * 1000L

        timerJob = scope.launch {
            var remaining = totalMs
            while (isActive && remaining > 0) {
                _sleepRemainingMs.value = remaining
                delay(1000)
                if (isPlaying()) {
                    remaining -= 1000
                }
            }
            if (isActive) {
                _sleepRemainingMs.value = 0L
                onTimerComplete()
                _sleepOption.value = SleepOption.OFF
                _sleepRemainingMs.value = null
            }
        }
    }

    fun setSleepOption(option: SleepOption, durationMinutes: Int = 0) {
        _sleepOption.value = option
        timerJob?.cancel()
        _sleepRemainingMs.value = null

        if (option != SleepOption.OFF && option != SleepOption.END_OF_TRACK && durationMinutes > 0) {
            val totalMs = durationMinutes * 60 * 1000L
            _sleepRemainingMs.value = totalMs
            timerJob = scope.launch {
                var remaining = totalMs
                while (remaining > 0 && isActive) {
                    delay(1000)
                    if (isPlaying()) {
                        remaining -= 1000
                        _sleepRemainingMs.value = remaining
                    }
                }

                if (isActive) {
                    _sleepRemainingMs.value = 0L
                    onTimerComplete()
                    _sleepOption.value = SleepOption.OFF
                    _sleepRemainingMs.value = null
                }
            }
        }
    }

    fun cancel() {
        timerJob?.cancel()
        timerJob = null
    }
}