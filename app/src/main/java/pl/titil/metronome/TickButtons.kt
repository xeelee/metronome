package pl.titil.metronome

import android.graphics.drawable.Drawable
import androidx.appcompat.widget.AppCompatButton

class TickButtons(private var buttons: ArrayList<AppCompatButton>) {
    fun bindModel(model: TicksViewModel) {
        buttons.forEachIndexed { index, btn ->
            btn.setOnClickListener {
                model.switchToNextState(index)
            }
        }
    }

    fun setCurrent(index: Int, getBgColor: (Int) -> Int, colorCurrent: Int) {
        buttons.forEachIndexed { idx, btn ->
            btn.background.setTint(getBgColor(idx))
        }
        val btn = buttons[index]
        btn.background.setTint(colorCurrent)
    }

    fun reconfigureDisplay(states: ArrayList<TickState>, getBgColor: (Int) -> Int,
                           soft: Drawable, hard: Drawable) {
        states.forEachIndexed { index, tickState ->
            val btn = buttons[index]
            btn.background.setTint(getBgColor(index))
            btn.foreground = when(tickState) {
                TickState.SOFT -> soft
                TickState.HARD -> hard
                else -> null
            }
        }
    }
}