package com.quran.labs.androidquran.ui.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.ListPreference

/**
 * A list preference with a handful of short choices. The settings screen draws it as a row of
 * segmented buttons, so changing it is one tap rather than opening a dialog.
 */
class SegmentedListPreference(context: Context, attrs: AttributeSet?) : ListPreference(context, attrs)
