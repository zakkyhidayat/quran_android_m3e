package com.quran.labs.androidquran.ui.preference

import android.content.Context
import android.util.AttributeSet

class SeekBarAyahTextSizePreference(
  context: Context, attrs: AttributeSet
) : SeekBarPreference(context, attrs) {

  override val preview: Preview = Preview.TEXT_SIZE
}
