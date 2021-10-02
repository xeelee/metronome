package pl.titil.metronome

import androidx.appcompat.widget.AppCompatImageView

class BeatAnimator(private val image: AppCompatImageView) {
    fun animate(interval: Long, beat: Int) {
        val animTime = interval / 5
        image.animate().apply {
            duration = animTime
            alpha(1f)
            with(image.width - 250) {
                val pos = (beat % 2) * this - this / 2
                translationX(pos.toFloat())
            }
        }.withEndAction {
            image.animate().apply {
                duration = animTime * 6
                alpha(0f)
            }
        }
    }
}