package com.quran.labs.androidquran.ui.preference

import android.content.Context
import android.util.AttributeSet

class SeekBarBackgroundBrightnessPreference(
  context: Context, attrs: AttributeSet
) : SeekBarPreference(context, attrs) {

  override val preview: Preview = Preview.BACKGROUND_BRIGHTNESS
}
