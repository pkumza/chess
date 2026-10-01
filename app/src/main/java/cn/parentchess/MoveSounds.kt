package cn.parentchess

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/** Short offline samples, played at landing. Never changes the device volume. */
class MoveSounds(context: Context) {
    private val loaded = mutableSetOf<Int>()
    private val pool = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    ).build()
    private val streams = mutableListOf<Int>()
    private val move: Int
    private val capture: Int
    private val click: Int
    private val success: Int
    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) loaded.add(id) }
        move = pool.load(context, R.raw.piece_move, 1)
        capture = pool.load(context, R.raw.piece_capture, 1)
        click = pool.load(context, R.raw.button_click, 1)
        success = pool.load(context, R.raw.lesson_success, 1)
    }
    fun success() {
        if (success in loaded) streams += pool.play(success, 0.90f, 0.90f, 2, 0, 1f)
    }
    fun click() {
        if (click in loaded) streams += pool.play(click, 0.85f, 0.85f, 2, 0, 1f)
        if (streams.size > 16) streams.removeAt(0)
    }
    fun play(isCapture: Boolean) {
        val id = if (isCapture) capture else move
        if (id in loaded) { streams.removeAll { it == 0 }; if (streams.size > 8) streams.removeAt(0); streams += pool.play(id, 0.95f, 0.95f, 1, 0, 1f) }
    }
    fun stop() { streams.forEach { pool.stop(it) }; streams.clear() }
    fun release() { pool.release(); loaded.clear() }
}
