package com.quran.labs.androidquran.common.ui.core

import androidx.compose.runtime.Composable

/**
 * A preference that draws itself with Compose. The settings screen renders [Content] as a
 * full-width row instead of building the preference's own view.
 */
interface ComposePreferenceRow {
  @Composable
  fun Content()
}
