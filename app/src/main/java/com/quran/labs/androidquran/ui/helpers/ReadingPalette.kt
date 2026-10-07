package com.quran.labs.androidquran.ui.helpers

import com.quran.labs.androidquran.data.Constants

/**
 * The colors of a reading mode: the page's paper, a card that sits on it (the ayah window), the ink
 * of the text and icons on both, and the green that marks what is on.
 */
class ReadingPalette(val paper: Int, val card: Int, val ink: Int, val accent: Int) {
  companion object {
    private val LIGHT = ReadingPalette(0xFFFDFBEF.toInt(), 0xFFEFEBD8.toInt(), 0xFF1F1D17.toInt(), 0xFF176B4D.toInt())
    private val SEPIA = ReadingPalette(0xFFF4E8CC.toInt(), 0xFFE8D8B0.toInt(), 0xFF3B2F1E.toInt(), 0xFF176B4D.toInt())
    private val NIGHT = ReadingPalette(0xFF000000.toInt(), 0xFF2B2B29.toInt(), 0xFFE3DED3.toInt(), 0xFF7FDBB0.toInt())

    @JvmStatic
    fun forMode(mode: String): ReadingPalette = when (mode) {
      Constants.READING_MODE_NIGHT -> NIGHT
      Constants.READING_MODE_SEPIA -> SEPIA
      else -> LIGHT
    }
  }
}
