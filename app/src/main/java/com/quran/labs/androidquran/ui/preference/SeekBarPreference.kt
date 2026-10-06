package com.quran.labs.androidquran.ui.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import com.quran.labs.androidquran.data.Constants

/**
 * A preference holding an int in 0..[maxValue]. It carries only the model (range, suffix, what to
 * preview); the settings screen draws the slider.
 */
open class SeekBarPreference(
  context: Context,
  attrs: AttributeSet
) : Preference(context, attrs) {

  /** What the sample under the slider shows. */
  enum class Preview { NONE, TEXT_SIZE }

  val suffix: String? = attrs.getAttributeValue(ANDROID_NS, "text")
  val default: Int = attrs.getAttributeIntValue(ANDROID_NS, "defaultValue", Constants.DEFAULT_TEXT_SIZE)
  val maxValue: Int = attrs.getAttributeIntValue(ANDROID_NS, "max", 100)

  open val preview: Preview = Preview.NONE

  fun currentValue(): Int = if (shouldPersist()) getPersistedInt(default) else default

  fun commitValue(value: Int) {
    if (shouldPersist()) {
      persistInt(value)
      callChangeListener(value)
    }
  }

  private companion object {
    private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
  }
}
