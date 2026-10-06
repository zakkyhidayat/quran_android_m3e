package com.quran.labs.androidquran.util

import android.app.Activity
import androidx.appcompat.app.AppCompatDelegate
import com.quran.labs.androidquran.common.ui.core.QuranThemeSettings
import com.quran.labs.androidquran.data.Constants

object ThemeUtil {

  fun setTheme(theme: String) {
    val mappedTheme = when (theme) {
      Constants.THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
      Constants.THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
      Constants.THEME_DEFAULT -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
      else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
    AppCompatDelegate.setDefaultNightMode(mappedTheme)
  }

  /**
   * Switch between the dynamic (wallpaper based) and the original color scheme. Compose screens
   * pick this up right away; the view based screens read their colors from the activity theme, so
   * every running activity is recreated to apply the new palette.
   */
  fun setColorScheme(colorScheme: String, activities: Collection<Activity> = emptyList()) {
    val useDynamicColor = colorScheme == Constants.COLOR_SCHEME_DYNAMIC
    if (QuranThemeSettings.useDynamicColor != useDynamicColor) {
      QuranThemeSettings.useDynamicColor = useDynamicColor
      activities.forEach { it.recreate() }
    }
  }
}
