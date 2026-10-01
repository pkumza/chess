package cn.parentchess

import android.content.Context
import android.media.AudioAttributes
import android.os.Handler
import android.os.Bundle
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/** Device Chinese TTS; no microphone, network permission, or forced volume changes. */
class LessonNarrator(context: Context, private val onState: (Boolean, String?) -> Unit) {
    private val main = Handler(Looper.getMainLooper())
    private val queue = NarrationQueue()
    private var engine: TextToSpeech? = null
    private var ready = false
    private var closed = false
    private var error: String? = null

    init {
        engine = TextToSpeech(context.applicationContext) { status ->
            main.post {
                if (!closed) initialize(status)
            }
        }
    }

    private fun initialize(status: Int) {
        val tts = engine ?: return
        if (status != TextToSpeech.SUCCESS) { unavailable(); return }
        val language = tts.setLanguage(Locale.SIMPLIFIED_CHINESE)
        if (language < TextToSpeech.LANG_AVAILABLE) { unavailable(); return }
        val voice = tts.voices.orEmpty().filter {
            it.locale.language == "zh" && !it.isNetworkConnectionRequired &&
                TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features.orEmpty()
        }.sortedWith(compareByDescending<android.speech.tts.Voice> { it.locale.country == "CN" }.thenByDescending { it.quality }).firstOrNull()
        if (voice != null) {
            if (tts.setVoice(voice) == TextToSpeech.ERROR) { unavailable(); return }
        } else if (tts.voice == null || tts.voice.isNetworkConnectionRequired || tts.voice.locale.language != "zh") {
            unavailable(); return
        }
        tts.setSpeechRate(.85f)
        tts.setPitch(1.0f)
        tts.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) { update(id, true, null) }
            override fun onDone(id: String?) { update(id, false, null) }
            @Deprecated("Legacy TTS callback")
            override fun onError(id: String?) { update(id, false, "朗读没有播放出来，请再试一次。") }
            override fun onError(id: String?, errorCode: Int) { onError(id) }
        })
        ready = true
        Log.i("LessonNarrator", "Chinese offline voice ready: ${tts.voice?.name}")
        onState(false, null)
        flush()
    }

    private fun update(id: String?, speaking: Boolean, message: String?) {
        main.post { if (!closed && queue.isCurrent(id)) onState(speaking, message) }
    }
    private fun unavailable() {
        error = "设备还没有可用的离线中文语音，请在系统文字转语音设置中检查中文语音包。"
        queue.clear()
        Log.w("LessonNarrator", "No usable offline Chinese voice")
        onState(false, error)
    }
    fun speak(text: String) {
        if (closed) return
        queue.replace(LessonSpeechText.normalize(text))
        if (error != null) { onState(false, error); return }
        onState(true, null)
        if (ready) flush()
    }
    private fun flush() {
        val (id, text) = queue.take() ?: return
        val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, .40f) }
        if (engine?.speak(text, TextToSpeech.QUEUE_FLUSH, params, id.toString()) == TextToSpeech.ERROR)
            onState(false, "朗读没有播放出来，请再试一次。")
    }
    fun stop() { queue.clear(); engine?.stop(); onState(false, error) }
    fun release() { closed = true; queue.clear(); engine?.stop(); engine?.shutdown(); engine = null }
}
