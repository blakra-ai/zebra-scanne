package com.stankovic.zebrascanner.scanning

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Audible + haptic result feedback so warehouse staff know the outcome without looking at the screen:
 * one short beep for success, a long double buzz for failure.
 */
object ScanFeedback {
    private val tone: ToneGenerator? by lazy {
        try {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        } catch (e: RuntimeException) {
            Log.w("ScanFeedback", "ToneGenerator unavailable: ${e.message}")
            null
        }
    }

    fun success(context: Context) {
        tone?.startTone(ToneGenerator.TONE_PROP_ACK, 150)
        vibrate(context, longArrayOf(0, 80))
    }

    fun failure(context: Context) {
        tone?.startTone(ToneGenerator.TONE_SUP_ERROR, 600)
        vibrate(context, longArrayOf(0, 300, 150, 300))
    }

    private fun vibrate(context: Context, pattern: LongArray) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator?.hasVibrator() != true) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            Log.w("ScanFeedback", "Vibration failed: ${e.message}")
        }
    }
}
