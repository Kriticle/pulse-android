package com.pulse.app

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Build

object HapticFeedback {
    fun light(context: Context) {
        vibrate(context, 10, 50)
    }

    fun medium(context: Context) {
        vibrate(context, 20, 100)
    }

    fun heavy(context: Context) {
        vibrate(context, 40, 200)
    }

    fun success(context: Context) {
        // Double tap pattern
        val pattern = longArrayOf(0, 20, 60, 20)
        vibratePattern(context, pattern)
    }

    private fun vibrate(context: Context, amplitude: Int, duration: Long) {
        val vibrator = getVibrator(context) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(duration, amplitude)
            )
        }
    }

    private fun vibratePattern(context: Context, pattern: LongArray) {
        val vibrator = getVibrator(context) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        }
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}