package pl.titil.metronome

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.widget.TextView

class TextColorAnimator(textView: TextView, toColor: Int) {
    private var initialColor: Int = 0
    private val animatorF: ObjectAnimator
    private val animatorB: ObjectAnimator

    init {
        val rgbEvaluator = ArgbEvaluator()
        initialColor = textView.currentTextColor
        animatorF = ObjectAnimator.ofInt(
            textView, "textColor",
            textView.currentTextColor, toColor).apply {
                setEvaluator(rgbEvaluator)
                duration = 50
            }
        animatorB = ObjectAnimator.ofInt(
            textView, "textColor",
            toColor, initialColor).apply {
                setEvaluator(rgbEvaluator)
                duration = 500
            }
    }
    fun forward() {
        animatorF.start()
    }

    fun back() {
        animatorB.start()
    }
}
