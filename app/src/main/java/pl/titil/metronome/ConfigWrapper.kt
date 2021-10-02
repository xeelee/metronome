package pl.titil.metronome

import java.lang.IndexOutOfBoundsException

class ConfigWrapper(
    private var config: ArrayList<Int>,
    private val drumID: Int,
    private val pulseID: Int) {

    companion object {
        const val NUM_ELEMENTS = 4
        const val SILENCE = -1
    }

    fun get(): ArrayList<TickState> = config.map {
        when (it) {
            pulseID -> TickState.SOFT
            drumID -> TickState.HARD
            SILENCE -> TickState.SILENT
            else -> TickState.NONE
        }
    } as ArrayList<TickState>

    fun set(value: ArrayList<TickState>) {
        value.forEachIndexed() { index, tickState ->
            val element = when (tickState) {
                TickState.SOFT -> pulseID
                TickState.HARD -> drumID
                TickState.SILENT -> SILENCE
                else -> 0
            }
            try {
                config[index] = element
            } catch (e: IndexOutOfBoundsException) {
                config.add(element)
            }
        }
    }

    fun isSet(): Boolean {
        return config.size >= NUM_ELEMENTS
    }
}