package com.quran.labs.androidquran.pages.common.madani.size

import com.quran.data.source.PageSizeCalculator

/** The tajweed pages come in one size, used on every screen. */
class TajweedPageSizeCalculator : PageSizeCalculator {
  override fun getWidthParameter() = "1280"

  override fun getTabletWidthParameter() = getWidthParameter()

  override fun setOverrideParameter(parameter: String) {
    // there is only one size
  }
}
