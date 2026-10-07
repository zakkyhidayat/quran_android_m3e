package com.quran.labs.androidquran.view

import android.os.Build
import android.view.View
import com.quran.labs.androidquran.R

/** The slim rounded scrollbar the long texts (the translation pages and the ayah window) share. */
object ScrollbarStyle {
  @JvmStatic
  fun show(view: View) {
    view.isVerticalScrollBarEnabled = true
    view.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
    view.isScrollbarFadingEnabled = true
    view.scrollBarDefaultDelayBeforeFade = 900
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      view.verticalScrollbarThumbDrawable = view.context.getDrawable(R.drawable.scrollbar_thumb)
    }
  }
}
