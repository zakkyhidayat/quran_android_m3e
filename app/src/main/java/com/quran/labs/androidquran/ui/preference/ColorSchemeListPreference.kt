package com.quran.labs.androidquran.ui.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.ListPreference

/**
 * The choice between wallpaper colors and the original green. The settings screen draws it as two
 * cards with swatches, the way the first-run setup does, rather than as a dialog.
 */
class ColorSchemeListPreference(context: Context, attrs: AttributeSet?) : ListPreference(context, attrs)
