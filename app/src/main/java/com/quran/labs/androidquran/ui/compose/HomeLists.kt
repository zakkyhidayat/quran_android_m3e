package com.quran.labs.androidquran.ui.compose

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.quran.data.core.QuranInfo
import com.quran.data.dao.ReadingBookmarksDao
import com.quran.data.model.bookmark.EmptyReadingBookmark
import com.quran.data.model.bookmark.ReadingBookmark
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.data.Constants
import com.quran.labs.androidquran.data.Constants.JUZ2_COUNT
import com.quran.labs.androidquran.data.Constants.SURAS_COUNT
import com.quran.labs.androidquran.data.QuranDisplayData
import com.quran.labs.androidquran.data.QuranFileConstants
import com.quran.labs.androidquran.presenter.data.JuzListPresenter
import com.quran.labs.androidquran.ui.helpers.QuranRow
import com.quran.labs.androidquran.ui.helpers.QuranRowFactory
import com.quran.labs.androidquran.util.QuranSettings
import com.quran.labs.androidquran.util.QuranUtils
import com.quran.labs.androidquran.view.JuzView
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * The surah tab's state. This is the logic of the old `SuraListFragment`: the 114 surahs grouped
 * under their juz, with the reading bookmarks (if any) pinned above them.
 */
class SuraListState(
  private val context: Context,
  private val quranInfo: QuranInfo,
  private val quranDisplayData: QuranDisplayData,
  private val quranSettings: QuranSettings,
  private val readingBookmarksDao: ReadingBookmarksDao,
  private val quranRowFactory: QuranRowFactory
) {
  private var readingBookmarks: List<ReadingBookmark> = emptyList()

  var rows by mutableStateOf<List<QuranRow>>(buildRows())
    private set

  /** Keeps the pinned reading bookmarks current; run it while the screen is started. */
  suspend fun observeReadingBookmarks() {
    readingBookmarksDao.readingBookmarksFlow()
      .distinctUntilChanged()
      .collect { updated ->
        val placed = placed(updated)
        if (readingBookmarks != placed) {
          readingBookmarks = placed
          rows = buildRows()
        }
      }
  }

  /**
   * Refreshes the rows when the screen comes back (the surah name setting may have changed), and
   * returns the row to scroll to so the surah you were last reading is in view, or null.
   */
  suspend fun onResume(latestPage: suspend () -> Int): Int? {
    readingBookmarks = placed(readingBookmarksDao.readingBookmarks())
    rows = buildRows()
    val recentPage = latestPage()
    if (recentPage == Constants.NO_PAGE) return null
    val sura = quranDisplayData.safelyGetSuraOnPage(recentPage)
    val juz = quranInfo.getJuzFromPage(recentPage)
    return sura + juz - 1 + readingBookmarkOffset()
  }

  private fun buildRows(): List<QuranRow> {
    val elements = ArrayList<QuranRow>(SURAS_COUNT + JUZ2_COUNT + readingBookmarkOffset())

    if (readingBookmarks.isNotEmpty()) {
      elements += quranRowFactory.fromReadingBookmarkHeader(context, readingBookmarks.size)
      readingBookmarks.forEach { elements += quranRowFactory.fromReadingBookmark(context, it) }
    }

    var sura = 1
    val wantPrefix = context.resources.getBoolean(R.bool.show_surat_prefix)
    val wantTranslation = quranSettings.isShowSuraTranslatedName
    for (juz in 1..JUZ2_COUNT) {
      elements += QuranRow.Builder()
        .withType(QuranRow.HEADER)
        .withText(context.getString(R.string.juz2_description, QuranUtils.getLocalizedNumber(juz)))
        .withPage(quranInfo.getStartingPageForJuz(juz))
        .build()

      val next = if (juz == JUZ2_COUNT) {
        quranInfo.numberOfPages + 1
      } else {
        quranInfo.getStartingPageForJuz(juz + 1)
      }

      while (sura <= SURAS_COUNT && quranInfo.getPageNumberForSura(sura) < next) {
        elements += QuranRow.Builder()
          .withText(quranDisplayData.getSuraName(context, sura, wantPrefix, wantTranslation))
          .withMetadata(quranDisplayData.getSuraListMetaString(context, sura))
          .withSura(sura)
          .withPage(quranInfo.getPageNumberForSura(sura))
          .build()
        sura++
      }
    }
    return elements
  }

  private fun placed(readingBookmarks: List<ReadingBookmark>): List<ReadingBookmark> =
    readingBookmarks
      .filterNot { it is EmptyReadingBookmark }
      .sortedBy { it.slot }

  private fun readingBookmarkOffset(): Int =
    if (readingBookmarks.isEmpty()) 0 else readingBookmarks.size + 1
}

/** The juz tab's state: every juz as a header followed by its eight quarters. */
class JuzListState(
  private val context: Context,
  private val quranInfo: QuranInfo,
  private val quranDisplayData: QuranDisplayData,
  private val juzListPresenter: JuzListPresenter
) {
  var rows by mutableStateOf<List<QuranRow>>(emptyList())
    private set

  suspend fun load() {
    val quarters = if (QuranFileConstants.FETCH_QUARTER_NAMES_FROM_DATABASE) {
      juzListPresenter.quarters().toTypedArray()
    } else {
      context.resources.getStringArray(R.array.quarter_prefix_array)
    }
    if (quarters.isNotEmpty()) rows = buildRows(quarters)
  }

  /** The row of the juz [page] is in, so it can be scrolled into view. */
  fun positionFor(page: Int): Int = (quranInfo.getJuzFromPage(page) - 1) * 9

  private fun buildRows(quarters: Array<String>): List<QuranRow> {
    val elements = ArrayList<QuranRow>(JUZ2_COUNT * (8 + 1))
    for (i in 0 until 8 * JUZ2_COUNT) {
      val pos = quranInfo.getQuarterByIndex(i)
      val page = quranInfo.getPageFromSuraAyah(pos.sura, pos.ayah)
      if (i % 8 == 0) {
        val juz = 1 + i / 8
        elements += QuranRow.Builder()
          .withType(QuranRow.HEADER)
          .withText(
            context.getString(R.string.juz2_description, QuranUtils.getLocalizedNumber(juz))
          )
          .withPage(quranInfo.getStartingPageForJuz(juz))
          .build()
      }
      val builder = QuranRow.Builder()
        .withText(quarters[i] + "...")
        .withMetadata(
          context.getString(
            R.string.sura_ayah_notification_str,
            quranDisplayData.getSuraName(context, pos.sura, false),
            pos.ayah
          )
        )
        .withPage(page)
        .withJuzType(ENTRY_TYPES[i % 4])
      if (i % 4 == 0) {
        builder.withJuzOverlayText(QuranUtils.getLocalizedNumber(1 + i / 4))
      }
      elements += builder.build()
    }
    return elements
  }

  private companion object {
    private val ENTRY_TYPES = intArrayOf(
      JuzView.TYPE_JUZ, JuzView.TYPE_QUARTER,
      JuzView.TYPE_HALF, JuzView.TYPE_THREE_QUARTERS
    )
  }
}
