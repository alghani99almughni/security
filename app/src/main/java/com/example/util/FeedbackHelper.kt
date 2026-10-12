package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class FeedbackHelper(context: Context) {

  private val appContext = context.applicationContext

  private val toneGenerator: ToneGenerator? by lazy {
    try {
      ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
    } catch (_: Exception) {
      null
    }
  }

  private val vibrator: Vibrator? by lazy {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
      } else {
        @Suppress("DEPRECATION")
        appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
      }
    } catch (_: Exception) {
      null
    }
  }

  /**
   * Sound and Vibration for APPROVE (Green / Positive Acknowledgment)
   */
  fun triggerApproveFeedback() {
    try {
      toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 250)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator?.vibrate(VibrationEffect.createOneShot(140, VibrationEffect.DEFAULT_AMPLITUDE))
      } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(140)
      }
    } catch (_: Exception) {
      // Graceful fallback
    }
  }

  /**
   * Sound and Vibration for REJECT (Red / Warning Buzz)
   */
  fun triggerRejectFeedback() {
    try {
      toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 350)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val pattern = longArrayOf(0, 100, 70, 180)
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
      } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(280)
      }
    } catch (_: Exception) {
      // Graceful fallback
    }
  }

  /**
   * Sound and Vibration for QR Code Scan Success
   */
  fun triggerScanSuccessFeedback() {
    try {
      toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator?.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))
      } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(70)
      }
    } catch (_: Exception) {
      // Graceful fallback
    }
  }
}
