package com.quran.labs.androidquran.ui.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference

/** The app logo with its name and description at the top of the about screen. */
class QuranHeaderPreference @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0,
  defStyleRes: Int = 0,
) : Preference(context, attrs, defStyleAttr, defStyleRes) {

  init {
    isSelectable = false
  }
}
