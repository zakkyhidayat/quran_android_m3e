package com.quran.labs.androidquran.ui.translation

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.style.ReplacementSpan
import kotlin.math.max

/**
 * A footnote marker drawn as a small rounded chip at the size of the text around it, instead of a
 * tiny superscript: easy to read and easy to tap.
 */
class FootnoteChipSpan(
  private val textColor: Int,
  private val chipColor: Int,
  private val horizontalPadding: Float,
  private val minWidth: Float,
  private val margin: Float
) : ReplacementSpan() {

  override fun getSize(
    paint: Paint,
    text: CharSequence,
    start: Int,
    end: Int,
    fm: Paint.FontMetricsInt?
  ): Int {
    val chip = max(paint.measureText(text, start, end) + 2 * horizontalPadding, minWidth)
    return (chip + 2 * margin).toInt()
  }

  override fun draw(
    canvas: Canvas,
    text: CharSequence,
    start: Int,
    end: Int,
    x: Float,
    top: Int,
    y: Int,
    bottom: Int,
    paint: Paint
  ) {
    val textWidth = paint.measureText(text, start, end)
    val chipWidth = max(textWidth + 2 * horizontalPadding, minWidth)
    val metrics = paint.fontMetrics
    val chipTop = y + metrics.ascent
    val chipBottom = y + metrics.descent
    val radius = (chipBottom - chipTop) / 2f

    val originalColor = paint.color
    paint.color = chipColor
    canvas.drawRoundRect(RectF(x + margin, chipTop, x + margin + chipWidth, chipBottom), radius, radius, paint)
    paint.color = textColor
    canvas.drawText(text, start, end, x + margin + (chipWidth - textWidth) / 2f, y.toFloat(), paint)
    paint.color = originalColor
  }
}
