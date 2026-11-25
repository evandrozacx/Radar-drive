package com.example.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

class VoiceAnnouncer(context: Context) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val navAudioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private var tts: TextToSpeech? = null
    private var pendingUtterance: String? = null
    private var focusRequest: AudioFocusRequest? = null

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _lastSpokenPhrase = MutableStateFlow<String?>(null)
    val lastSpokenPhrase: StateFlow<String?> = _lastSpokenPhrase.asStateFlow()

    init {
        initTts()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(appContext, this).apply {
                setAudioAttributes(navAudioAttributes)
                setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onDone(utteranceId: String?) {
                        abandonAudioFocus()
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        abandonAudioFocus()
                    }
                    override fun onError(utteranceId: String?, errorCode: Int) {
                        abandonAudioFocus()
                    }
                })
            }
        } catch (_: Exception) {
            _isReady.value = false
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val engine = tts ?: return
            engine.setAudioAttributes(navAudioAttributes)
            val ptBr = Locale("pt", "BR")
            val langResult = engine.setLanguage(ptBr)
            if (langResult == TextToSpeech.LANG_MISSING_DATA ||
                langResult == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                engine.setLanguage(Locale("pt"))
            }
            engine.setSpeechRate(1.06f)
            _isReady.value = true
            pendingUtterance?.let { phrase ->
                pendingUtterance = null
                speak(phrase)
            }
        } else {
            _isReady.value = false
        }
    }

    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
        if (muted) {
            stop()
        }
    }

    fun speak(phrase: String) {
        _lastSpokenPhrase.value = phrase
        if (_isMuted.value) return
        val engine = tts
        if (engine == null || !_isReady.value) {
            pendingUtterance = phrase
            return
        }
        requestNavAudioFocus()
        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        }
        engine.setAudioAttributes(navAudioAttributes)
        engine.speak(phrase, TextToSpeech.QUEUE_FLUSH, params, UUID.randomUUID().toString())
    }

    fun stop() {
        pendingUtterance = null
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
        abandonAudioFocus()
    }

    private fun requestNavAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(navAudioAttributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener { }
                .build()
            focusRequest = request
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            focusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    fun shutdown() {
        stop()
        try {
            tts?.shutdown()
        } catch (_: Exception) {
        }
        tts = null
    }
}
