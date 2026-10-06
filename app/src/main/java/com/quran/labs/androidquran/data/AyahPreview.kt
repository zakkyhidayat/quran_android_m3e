package com.quran.labs.androidquran.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.quran.labs.androidquran.util.QuranFileUtils
import timber.log.Timber
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Cuts the first lines of one ayah out of a downloaded page image, to show what a page style
 * looks like. Returns null when the page or its ayah positions are not on the phone yet.
 */
object AyahPreview {

  fun load(
    quranFileUtils: QuranFileUtils,
    widthParam: String,
    page: Int,
    sura: Int,
    ayah: Int,
    maxLines: Int = 3
  ): Bitmap? {
    return try {
      val image = File(
        quranFileUtils.getQuranImagesDirectory(widthParam),
        QuranFileUtils.getPageFileName(page)
      )
      if (!image.exists()) return null

      val handler = AyahInfoDatabaseHandler.getAyahInfoDatabaseHandler(
        quranFileUtils.getAyaPositionFileName(widthParam),
        quranFileUtils
      ) ?: return null
      val lines = handler.getVersesBoundsForPage(page).ayahCoordinates["$sura:$ayah"]
        ?.sortedBy { it.line }
        ?.take(maxLines)
      if (lines.isNullOrEmpty()) return null

      val bitmap = BitmapFactory.decodeFile(image.absolutePath) ?: return null
      // a little room above and below, so the marks over and under the letters are kept
      val lineHeight = lines.first().bounds.height()
      val top = max(0, (lines.minOf { it.bounds.top } - lineHeight * 0.15f).roundToInt())
      val bottom = min(bitmap.height, (lines.maxOf { it.bounds.bottom } + lineHeight * 0.15f).roundToInt())
      if (bottom <= top) return null
      Bitmap.createBitmap(bitmap, 0, top, bitmap.width, bottom - top)
    } catch (e: Exception) {
      Timber.e(e, "unable to make an ayah preview")
      null
    }
  }
}
