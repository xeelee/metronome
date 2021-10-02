package pl.titil.metronome

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class SoundPlayer(private val pool: SoundPool, val drumID: Int, val pulseID: Int) {
    companion object {
        fun create(context: Context, drumRes: Int, pulseRes: Int): SoundPlayer {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val pool = SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(attributes)
                .build()
            val drumID = pool.load(context, drumRes, 1)
            val pulseID = pool.load(context, pulseRes, 1)
            return SoundPlayer(pool, drumID, pulseID)
        }
    }

    fun play(id: Int) = pool.play(
        id, 1f, 1f, 1, 0, 1f)
}