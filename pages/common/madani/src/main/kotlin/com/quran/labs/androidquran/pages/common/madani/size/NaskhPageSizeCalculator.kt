package com.quran.labs.androidquran.pages.common.madani.size

import com.quran.data.source.DisplaySize
import com.quran.data.source.PageSizeCalculator

/** The Naskh pages come in three sizes; the larger ones are for bigger screens. */
class NaskhPageSizeCalculator(displaySize: DisplaySize) : PageSizeCalculator {
  private val maxWidth: Int = if (displaySize.x > displaySize.y) displaySize.x else displaySize.y

  override fun getWidthParameter() = when {
    maxWidth <= 1280 -> "1152"
    maxWidth <= 1920 -> "1280"
    else -> "1536"
  }

  override fun getTabletWidthParameter() = getWidthParameter()

  override fun setOverrideParameter(parameter: String) {
    // the size follows the screen
  }
}
