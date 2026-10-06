package com.quran.labs.androidquran.ui.fragment

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.quran.data.core.QuranInfo
import com.quran.data.dao.ReadingBookmarksDao
import com.quran.data.model.bookmark.EmptyReadingBookmark
import com.quran.data.model.bookmark.ReadingBookmark
import com.quran.labs.androidquran.QuranApplication
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.common.ui.core.QuranTheme
import com.quran.labs.androidquran.ui.compose.QuranRowList
import com.quran.labs.androidquran.data.Constants
import com.quran.labs.androidquran.data.Constants.JUZ2_COUNT
import com.quran.labs.androidquran.data.Constants.SURAS_COUNT
import com.quran.labs.androidquran.data.QuranDisplayData
import com.quran.labs.androidquran.ui.QuranActivity
import com.quran.labs.androidquran.ui.helpers.QuranRow
import com.quran.labs.androidquran.ui.helpers.QuranRowFactory
import com.quran.labs.androidquran.util.QuranSettings
import com.quran.labs.androidquran.util.QuranUtils
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class SuraListFragment : Fragment() {

  @Inject
  lateinit var quranInfo: QuranInfo

  @Inject
  lateinit var quranDisplayData: QuranDisplayData

  @Inject
  lateinit var quranSettings: QuranSettings

  @Inject
  lateinit var readingBookmarksDao: ReadingBookmarksDao

  @Inject
  lateinit var quranRowFactory: QuranRowFactory

  private val listState = LazyListState()
  private var rows by mutableStateOf<List<QuranRow>>(emptyList())
  private var numberOfPages = 0
  private var showSuraTranslatedName = false
  private var readingBookmarks: List<ReadingBookmark> = emptyList()

  override fun onAttach(context: Context) {
    super.onAttach(context)
    (context.applicationContext as QuranApplication).applicationComponent.inject(this)
    numberOfPages = quranInfo.numberOfPages
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    showSuraTranslatedName = quranSettings.isShowSuraTranslatedName
    rows = getSuraList().toList()

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        readingBookmarksDao.readingBookmarksFlow()
          .distinctUntilChanged()
          .collect { updateReadingBookmarks(it) }
      }
    }

    return ComposeView(requireContext()).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent {
        QuranTheme {
          QuranRowList(
            rows = rows,
            listState = listState,
            contentPadding = WindowInsets.navigationBars.asPaddingValues(),
            onRowClick = { _, row -> onRowClick(row) }
          )
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    val activity = requireActivity()
    if (activity is QuranActivity) {
      val newValueOfShowSuraTranslatedName = quranSettings.isShowSuraTranslatedName
      if (showSuraTranslatedName != newValueOfShowSuraTranslatedName) {
        showHideSuraTranslatedName()
        showSuraTranslatedName = newValueOfShowSuraTranslatedName
      }
      viewLifecycleOwner.lifecycleScope.launch {
        readingBookmarks = placedReadingBookmarks(readingBookmarksDao.readingBookmarks())
        updateSuraList()
        val recentPage = activity.latestPage()
        if (recentPage != Constants.NO_PAGE) {
          val sura = quranDisplayData.safelyGetSuraOnPage(recentPage)
          val juz = quranInfo.getJuzFromPage(recentPage)
          val position = sura + juz - 1 + readingBookmarkOffset()
          listState.scrollToItem(position)
        }
      }
    }
  }

  private fun getSuraList(): Array<QuranRow> {
    var next: Int
    var pos = 0
    var sura = 1
    val readingBookmarks = readingBookmarks
    val elements = arrayOfNulls<QuranRow>(SURAS_COUNT + JUZ2_COUNT + readingBookmarkOffset())

    val activity: Activity = requireActivity()
    if (readingBookmarks.isNotEmpty()) {
      elements[pos++] = quranRowFactory.fromReadingBookmarkHeader(activity, readingBookmarks.size)
      for (readingBookmark in readingBookmarks) {
        elements[pos++] = quranRowFactory.fromReadingBookmark(activity, readingBookmark)
      }
    }

    val wantPrefix = activity.resources.getBoolean(R.bool.show_surat_prefix)
    val wantTranslation = quranSettings.isShowSuraTranslatedName
    for (juz in 1..JUZ2_COUNT) {
      val headerTitle = activity.getString(
        R.string.juz2_description,
        QuranUtils.getLocalizedNumber(juz)
      )
      val headerBuilder = QuranRow.Builder()
        .withType(QuranRow.HEADER)
        .withText(headerTitle)
        .withPage(quranInfo.getStartingPageForJuz(juz))
      elements[pos++] = headerBuilder.build()
      next = if (juz == JUZ2_COUNT) {
        numberOfPages + 1
      } else quranInfo.getStartingPageForJuz(juz + 1)

      while (sura <= SURAS_COUNT && quranInfo.getPageNumberForSura(sura) < next) {
        val builder = QuranRow.Builder()
          .withText(quranDisplayData.getSuraName(activity, sura, wantPrefix, wantTranslation))
          .withMetadata(quranDisplayData.getSuraListMetaString(activity, sura))
          .withSura(sura)
          .withPage(quranInfo.getPageNumberForSura(sura))
        elements[pos++] = builder.build()
        sura++
      }
    }
    return elements.filterNotNull().toTypedArray()
  }

  private fun showHideSuraTranslatedName() {
    updateSuraList()
  }

  private fun updateReadingBookmarks(readingBookmarks: List<ReadingBookmark>) {
    val placed = placedReadingBookmarks(readingBookmarks)
    if (this.readingBookmarks != placed) {
      this.readingBookmarks = placed
      updateSuraList()
    }
  }

  private fun placedReadingBookmarks(
    readingBookmarks: List<ReadingBookmark>
  ): List<ReadingBookmark> {
    return readingBookmarks
      .filterNot { readingBookmark -> readingBookmark is EmptyReadingBookmark }
      .sortedBy { readingBookmark -> readingBookmark.slot }
  }

  private fun updateSuraList() {
    rows = getSuraList().toList()
  }

  private fun readingBookmarkOffset(): Int {
    return if (readingBookmarks.isEmpty()) 0 else readingBookmarks.size + 1
  }

  private fun onRowClick(row: QuranRow) {
    val activity = activity as? QuranActivity
    if (activity != null && row.page != 0) {
      activity.jumpTo(row)
    }
  }

  companion object {
    fun newInstance(): SuraListFragment = SuraListFragment()
  }
}
