package com.quran.labs.androidquran.ui.fragment

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.preference.PreferenceFragmentCompat

/**
 * A preference fragment that has no view of its own. It builds the [preferenceScreen] and holds
 * the screen's logic (listeners, side effects of a change); the hosting activity draws the
 * preferences with Compose, which keeps the app bar able to collapse with the list.
 */
abstract class QuranPreferenceFragment : PreferenceFragmentCompat() {
  private var changes by mutableIntStateOf(0)

  /** Read by the Compose list so it redraws when [invalidatePreferences] is called. */
  val changeCount: Int get() = changes

  /** Call after changing a preference outside of a tap: its summary, entries or enabled state. */
  fun invalidatePreferences() {
    changes++
  }

  companion object {
    const val TAG = "preferences"
  }
}
