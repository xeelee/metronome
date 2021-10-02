package pl.titil.metronome

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TicksViewModel : ViewModel() {

    private var ticks = arrayListOf(TickState.HARD, TickState.SOFT, TickState.SOFT, TickState.SOFT)

    val config = MutableLiveData<ArrayList<TickState>>().apply {
        postValue(ticks)
    }

    var bpm = MutableLiveData(60)

    fun getTickAt(index: Int) = ticks[index]

    fun setTickAt(index: Int, state: TickState) {
        ticks[index] = state
        config.postValue(ticks)
    }

    fun setConfig(states: ArrayList<TickState>) {
        ticks = states
        config.postValue(ticks)
    }

    fun switchToNextState(index: Int) {
        val currentTickState = getTickAt(index)
        setTickAt(index, when(currentTickState) {
            TickState.SOFT -> TickState.HARD
            TickState.HARD -> TickState.SILENT
            TickState.SILENT -> TickState.NONE
            TickState.NONE -> TickState.SOFT
        })
    }
}