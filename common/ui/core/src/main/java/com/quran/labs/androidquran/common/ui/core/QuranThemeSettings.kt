package com.quran.labs.androidquran.common.ui.core

import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * App wide theme choices that [QuranTheme] reads. These live in snapshot state, so every composable
 * using [QuranTheme] recomposes as soon as the app updates them - no need to recreate the screen.
 */
object QuranThemeSettings {

  /** Whether dynamic (wallpaper based) color can be used on this device at all. */
  val isDynamicColorAvailable: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

  /**
   * Whether to use dynamic color rather than the original Quran palette. Set by the app from the
   * user's color scheme preference. Ignored when [isDynamicColorAvailable] is false.
   */
  var useDynamicColor: Boolean by mutableStateOf(true)
}
