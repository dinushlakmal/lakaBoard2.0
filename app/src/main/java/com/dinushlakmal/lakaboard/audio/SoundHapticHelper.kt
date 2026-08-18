package com.dinushlakmal.lakaboard.audio

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

enum class SoundProfile { NONE, MECHANICAL_CLICK, SOFT_BUBBLE, MODERN_POP, TYPEWRITER }

/**
 * SoundHapticHelper
 * ---------------------------------------------------------------------
 * Robust, zero-overhead audio and haptic feedback engine.
 * Uses Android's native system sound effects (AudioManager.playSoundEffect)
 * to avoid MediaCodec, file decoders, or ashmem deprecation errors.
 */
class SoundHapticHelper(private val context: Context) {

    private var soundEnabled = true
    private var hapticEnabled = true
    private var volume = 0.8f

    private val audioManager: AudioManager? by lazy {
        try {
            context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        } catch (_: Throwable) {
            null
        }
    }

    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun setSoundEnabled(enabled: Boolean) { soundEnabled = enabled }
    fun setHapticEnabled(enabled: Boolean) { hapticEnabled = enabled }
    fun setVolume(v: Float) { volume = v.coerceIn(0f, 1f) }

    fun onKeyPress(profile: SoundProfile) {
        if (hapticEnabled) {
            vibrateTick()
        }
        if (soundEnabled && profile != SoundProfile.NONE) {
            playFx(profile)
        }
    }

    private fun playFx(profile: SoundProfile) {
        try {
            val am = audioManager ?: return
            val fxType = when (profile) {
                SoundProfile.MECHANICAL_CLICK -> AudioManager.FX_KEYPRESS_STANDARD
                SoundProfile.SOFT_BUBBLE -> AudioManager.FX_KEYPRESS_SPACEBAR
                SoundProfile.MODERN_POP -> AudioManager.FX_KEYPRESS_DELETE
                SoundProfile.TYPEWRITER -> AudioManager.FX_KEYPRESS_RETURN
                SoundProfile.NONE -> return
            }
            am.playSoundEffect(fxType, volume)
        } catch (_: Throwable) {}
    }

    private fun vibrateTick() {
        try {
            val v = vibrator ?: return
            if (!v.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(10)
            }
        } catch (_: Throwable) {}
    }

    fun release() {
        // No persistent native handles held
    }
}
