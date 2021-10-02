package pl.titil.metronome

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ControlReceiver(private val onReceive: (String?) -> Unit) : BroadcastReceiver() {
    companion object {
        const val ACTION_TOGGLE = "pl.titil.metronome.TOGGLE"
        const val ACTION_STOP = "pl.titil.metronome.STOP"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        onReceive(intent?.action)
    }
}