package com.dinushlakmal.lakaboard.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.min

enum class SoundProfile { NONE, MECHANICAL_CLICK, SOFT_BUBBLE, MODERN_POP, TYPEWRITER }

/**
 * SoundHapticHelper
 * ---------------------------------------------------------------------
 * Synthesizes short key-press click sounds entirely in-memory (no audio
 * assets required) using simple additive/envelope synthesis, and fires
 * matching haptic feedback. Mirrors the Web Audio oscillator recipes
 * used by the web simulator so both surfaces "feel" the same.
 */
class SoundHapticHelper(private val context: Context) {

    private val sampleRate = 44100
    private var soundEnabled = true
    private var hapticEnabled = true
    private var volume = 0.6f

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun setSoundEnabled(enabled: Boolean) { soundEnabled = enabled }
    fun setHapticEnabled(enabled: Boolean) { hapticEnabled = enabled }
    fun setVolume(v: Float) { volume = v.coerceIn(0f, 1f) }

    /** Play the click sound + haptic pulse for the given profile. */
    fun onKeyPress(profile: SoundProfile) {
        if (hapticEnabled) vibrateTick()
        if (soundEnabled && profile != SoundProfile.NONE) {
            Thread { playSynthesized(profile) }.start()
        }
    }

    private fun vibrateTick() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(12, 40))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(12)
        }
    }

    private fun playSynthesized(profile: SoundProfile) {
        val durationMs = when (profile) {
            SoundProfile.MECHANICAL_CLICK -> 35
            SoundProfile.SOFT_BUBBLE -> 90
            SoundProfile.MODERN_POP -> 60
            SoundProfile.TYPEWRITER -> 50
            SoundProfile.NONE -> return
        }
        val numSamples = sampleRate * durationMs / 1000
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val sample = when (profile) {
                SoundProfile.MECHANICAL_CLICK -> {
                    val freq = 1800.0
                    val env = exp(-progress * 18)
                    sin(2 * PI * freq * t) * env
                }
                SoundProfile.SOFT_BUBBLE -> {
                    val freq = 420.0 + 260.0 * progress
                    val env = sin(PI * progress) // rise then fall
                    sin(2 * PI * freq * t) * env
                }
                SoundProfile.MODERN_POP -> {
                    val freq = 900.0 * exp(-progress * 3)
                    val env = exp(-progress * 9)
                    sin(2 * PI * freq * t) * env
                }
                SoundProfile.TYPEWRITER -> {
                    val freq = 2200.0
                    val env = if (progress < 0.15) progress / 0.15 else exp(-(progress - 0.15) * 14)
                    // add a touch of noise for the mechanical "clack"
                    val noise = (Math.random() * 2 - 1) * 0.15
                    (sin(2 * PI * freq * t) + noise) * env
                }
                SoundProfile.NONE -> 0.0
            }
            buffer[i] = (sample * volume * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val minBufSize = AudioTrack.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val track = AudioTrack(
            attrs, format, min(minBufSize, buffer.size * 2).coerceAtLeast(buffer.size * 2),
            AudioTrack.MODE_STATIC, AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        track.write(buffer, 0, buffer.size)
        track.play()
        // Release once playback completes (short clips only).
        Thread {
            Thread.sleep((durationMs + 20).toLong())
            track.release()
        }.start()
    }

    fun release() {
        // No persistent resources held beyond per-click AudioTracks.
    }
}
