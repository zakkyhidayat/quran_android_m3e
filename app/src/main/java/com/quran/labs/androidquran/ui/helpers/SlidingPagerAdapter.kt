package com.quran.labs.androidquran.ui.helpers

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.quran.labs.androidquran.ui.fragment.AyahPlaybackFragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.quran.labs.androidquran.view.IconPageIndicator
import com.quran.mobile.di.AyahActionFragmentProvider

private class EmptyAyahPanel : Fragment() {
  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
    View(inflater.context)
}

private object TranslationSlot : AyahActionFragmentProvider {
  override val order = SlidingPagerAdapter.TRANSLATION_PAGE
  override val iconResId = com.quran.labs.androidquran.common.toolbar.R.drawable.ic_translation
  override fun newAyahActionFragment(): Fragment = EmptyAyahPanel()
}

class SlidingPagerAdapter(
  fm: FragmentManager,
  private val isRtl: Boolean,
  additionalPanels: Set<AyahActionFragmentProvider>,
  includeAudioPanel: Boolean = true,
) : FragmentStatePagerAdapter(fm, "sliding_without_tag"), IconPageIndicator.IconPagerAdapter {

  private val pages: ArrayList<AyahActionFragmentProvider> = arrayListOf()

  init {
    // Add the core ayah action panels
    // the translation moved into the floating ayah window; this slot only keeps the page numbers
    // of the other panels where they were
    pages.add(TranslationSlot)
    if (includeAudioPanel) {
      pages.add(AyahPlaybackFragment.Provider)
    }

    // Since additionalPanel Set may be unsorted, put them in a list and sort them by page number..
    val additionalPages: ArrayList<AyahActionFragmentProvider> = ArrayList(additionalPanels)
    additionalPages.sortWith { o1, o2 -> o1.order.compareTo(o2.order) }
    // ..then add them to the pages list
    pages.addAll(additionalPages)
  }

  override fun getIconResId(index: Int): Int {
    val pos = getPagePosition(index)
    return pages[pos].iconResId
  }

  override fun getCount(): Int = pages.size

  override fun getItem(position: Int): Fragment {
    val pos = getPagePosition(position)
    return pages[pos].newAyahActionFragment()
  }

  fun getPagePosition(page: Int): Int {
    return if (isRtl) pages.size - 1 - page else page
  }

  companion object {
    const val TRANSLATION_PAGE = 0
    const val AUDIO_PAGE = 1
    const val TRANSCRIPT_PAGE = 2
  }
}
