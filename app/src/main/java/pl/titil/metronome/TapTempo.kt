package pl.titil.metronome

import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch


class TapTempo(private val scope: LifecycleCoroutineScope) {
    companion object {
        const val MAX_INTERVAL: Long = 1665
        const val MIN_INTERVAL: Long = 250
    }

    private val lastTaps = mutableListOf<Long>()
    private val _tempo = MutableSharedFlow<Long>()
    val tempo: SharedFlow<Long> = _tempo.asSharedFlow()
    private var _lastMillis: Long = 0

    private var _isActive = false
    private val _active = MutableSharedFlow<Boolean>()
    val active: SharedFlow<Boolean> = _active.asSharedFlow()

    fun tap() {
        if (!_isActive) {
            _isActive = true
            scope.launch {
                _active.emit(_isActive)
                while (getMillisFromLastTap().second <= MAX_INTERVAL || _lastMillis == 0L) {
                    delay(200)
                }
                clear()
            }
        }
        while (lastTaps.size > 2) {
            lastTaps.removeAt(0)
        }
        val millis = getMillisFromLastTap()
        if (_lastMillis > 0) {
            var difference = millis.second
            if (difference > MAX_INTERVAL) {
                clear()
            } else {
                if (difference < MIN_INTERVAL){
                    difference = MIN_INTERVAL
                }
                lastTaps.add(difference)
                scope.launch {
                    _tempo.emit(lastTaps.average().toLong())
                }
            }
        }
        _lastMillis = millis.first
    }

    private fun clear() {
        if (!_isActive) return

        _isActive = false
        lastTaps.clear()
        _lastMillis = 0
        scope.launch {
            _active.emit(_isActive)
        }
    }

    private fun getMillisFromLastTap(): Pair<Long, Long> {
        val currentMillis = System.currentTimeMillis()
        return Pair(currentMillis, currentMillis - _lastMillis)
    }
}
