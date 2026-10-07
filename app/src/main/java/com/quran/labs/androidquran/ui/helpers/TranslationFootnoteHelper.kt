package com.quran.labs.androidquran.ui.helpers

import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned

object TranslationFootnoteHelper {

  fun footnoteCognizantText(
    data: CharSequence?,
    footnotes: List<IntRange>,
    spannableStringBuilder: SpannableStringBuilder,
    expandedFootnotes: List<Int>,
    collapsedFootnoteSpannableStyler: ((Int) -> SpannableString),
    expandedFootnoteSpannableStyler: ((SpannableStringBuilder, Int, Int) -> SpannableStringBuilder),
    expandedFootnoteClickSpan: ((Int) -> Any)? = null
  ): CharSequence {
    return if (data != null) {
      val ranges = footnotes.sortedByDescending { it.last }
      ranges.foldIndexed(spannableStringBuilder) { index, builder, range ->
        val number = ranges.size - index
        if (number !in expandedFootnotes) {
          builder.replace(
            range.first,
            range.last + 1,
            collapsedFootnoteSpannableStyler(number)
          )
        } else {
          val styled = expandedFootnoteSpannableStyler(builder, range.first, range.last + 1)
          expandedFootnoteClickSpan?.let {
            styled.setSpan(it(number), range.first, range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
          }
          styled
        }
      }
    } else {
      ""
    }
  }
}
