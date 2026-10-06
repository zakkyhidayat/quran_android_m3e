package com.quran.labs.androidquran.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
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
      // each line keeps only this ayah's part of it, set from the right as Arabic is read, so the
      // end of the previous ayah on the first line is left out
      val rects = lines.map { line ->
        val bounds = line.bounds
        val pad = bounds.height() * 0.15f
        Rect(
          max(0, bounds.left.roundToInt()),
          max(0, (bounds.top - pad).roundToInt()),
          min(bitmap.width, bounds.right.roundToInt()),
          min(bitmap.height, (bounds.bottom + pad).roundToInt())
        )
      }.filter { it.width() > 0 && it.height() > 0 }
      if (rects.isEmpty()) return null

      val left = rects.minOf { it.left }
      val right = rects.maxOf { it.right }
      val output = Bitmap.createBitmap(right - left, rects.sumOf { it.height() }, bitmap.config ?: Bitmap.Config.ARGB_8888)
      val canvas = Canvas(output)
      var y = 0
      rects.forEach { src ->
        val x = (right - left) - src.width()
        canvas.drawBitmap(bitmap, src, Rect(x, y, x + src.width(), y + src.height()), null)
        y += src.height()
      }
      output
    } catch (e: Exception) {
      Timber.e(e, "unable to make an ayah preview")
      null
    }
  }
}
