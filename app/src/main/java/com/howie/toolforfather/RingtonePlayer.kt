package com.howie.toolforfather

import android.content.Context
import android.media.*
import android.os.Build

object RingtonePlayer {

    private const val TYPE_AUDIO = AudioManager.STREAM_MUSIC

    private lateinit var player: MediaPlayer
    private lateinit var audioManager: AudioManager
    private var volumeMax: Int = 0
    private var volumeCurrent: Int = 0
    private val listener by lazy { AudioManager.OnAudioFocusChangeListener {} }
    private val focusRequest: AudioFocusRequest? by lazy {
        if (!needFocusRequest()) {
            return@lazy null
        }
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attributes)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener(listener)
            .build()
    }
    private var hasInit: Boolean = false


    fun init(context: Context) {
        if (hasInit) {
            return
        }
        // audioManager
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        volumeMax = audioManager.getStreamMaxVolume(TYPE_AUDIO)

        // player
        val ringtoneUri =
            RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
        player = MediaPlayer()
        player.setDataSource(context, ringtoneUri)
        player.isLooping = true
        player.prepareAsync()
        hasInit = true
    }

    fun start() {
        requestFocus()
        volumeCurrent = audioManager.getStreamVolume(TYPE_AUDIO)
        audioManager.setStreamVolume(TYPE_AUDIO, volumeMax, 0)
        player.start()
    }

    fun stop() {
        player.stop()
        audioManager.setStreamVolume(TYPE_AUDIO, volumeCurrent, 0)
        player.prepareAsync()
        releaseFocus()
    }

    fun destroy() {
        player.stop()
        player.release()
    }

    private fun requestFocus() {
        if (needFocusRequest()) {
            audioManager.requestAudioFocus(focusRequest!!)
        } else {
            audioManager.requestAudioFocus(
                listener,
                TYPE_AUDIO,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
        }
    }

    private fun releaseFocus() {
        if (needFocusRequest()) {
            audioManager.abandonAudioFocusRequest(focusRequest!!)
        } else {
            audioManager.abandonAudioFocus(listener)
        }
    }

    private fun needFocusRequest(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }
}