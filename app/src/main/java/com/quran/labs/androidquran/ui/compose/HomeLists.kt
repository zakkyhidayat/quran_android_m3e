package com.quran.labs.androidquran.ui.compose

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
 * The surah tab's state: the 114 surahs in one list, with the reading bookmarks (if any) pinned
 * above them.
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

  /** The surah the last read page belongs to, or 0 when nothing was read yet. */
  var lastReadSura by mutableIntStateOf(0)
    private set

  /** False until the page last read has been looked up, so the button doesn't show a guess. */
  var lastReadLoaded by mutableStateOf(false)
    private set

  /** The first ayah on the page last read. */
  var lastReadAyah by mutableIntStateOf(0)
    private set

  /** The page last read, or 0 when nothing was read yet. */
  var lastReadPage by mutableIntStateOf(0)
    private set

  /** Follows the page last read, so the highlighted surah changes as soon as you read another. */
  fun onLatestPage(page: Int) {
    lastReadLoaded = true
    lastReadPage = if (page == Constants.NO_PAGE) 0 else page
    lastReadAyah = if (page == Constants.NO_PAGE) 0 else runCatching { quranInfo.getFirstAyahOnPage(page) }.getOrDefault(0)
    lastReadSura = if (page == Constants.NO_PAGE) 0 else quranDisplayData.safelyGetSuraOnPage(page)
  }

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
   * Refreshes the rows when the screen comes back (the surah name setting may have changed) and
   * notes which surah was read last, so its row can be highlighted.
   */
  suspend fun onResume(latestPage: suspend () -> Int) {
    readingBookmarks = placed(readingBookmarksDao.readingBookmarks())
    rows = buildRows()
    val recentPage = latestPage()
    lastReadLoaded = true
    lastReadPage = if (recentPage == Constants.NO_PAGE) 0 else recentPage
    lastReadAyah = if (recentPage == Constants.NO_PAGE) 0 else runCatching { quranInfo.getFirstAyahOnPage(recentPage) }.getOrDefault(0)
    lastReadSura = if (recentPage == Constants.NO_PAGE) 0 else quranDisplayData.safelyGetSuraOnPage(recentPage)
  }

  private fun buildRows(): List<QuranRow> {
    val elements = ArrayList<QuranRow>(SURAS_COUNT + readingBookmarkOffset())

    if (readingBookmarks.isNotEmpty()) {
      elements += quranRowFactory.fromReadingBookmarkHeader(context, readingBookmarks.size)
      readingBookmarks.forEach { elements += quranRowFactory.fromReadingBookmark(context, it) }
    }

    // the list is the surahs, so "Surah" in front of every name only repeats itself
    val wantPrefix = false
    val wantTranslation = quranSettings.isShowSuraTranslatedName
    for (sura in 1..SURAS_COUNT) {
      elements += QuranRow.Builder()
        .withText(quranDisplayData.getSuraName(context, sura, wantPrefix, wantTranslation))
        .withMetadata(quranDisplayData.getSuraListMetaString(context, sura))
        .withSura(sura)
        .withPage(quranInfo.getPageNumberForSura(sura))
        .build()
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

/**
 * The hizb tab's state: the same 240 quarters as the juz tab, grouped by the 60 hizb instead. each
 * hizb is a header followed by its four quarters (the start, a quarter, a half and three quarters).
 */
class HizbListState(
  private val context: Context,
  private val quranInfo: QuranInfo,
  private val quranDisplayData: QuranDisplayData,
  private val juzListPresenter: JuzListPresenter
) {
  var rows by mutableStateOf<List<QuranRow>>(emptyList())
    private set

  private val quarterPages by lazy {
    IntArray(QUARTERS) { i ->
      val pos = quranInfo.getQuarterByIndex(i)
      quranInfo.getPageFromSuraAyah(pos.sura, pos.ayah)
    }
  }

  suspend fun load() {
    val quarters = if (QuranFileConstants.FETCH_QUARTER_NAMES_FROM_DATABASE) {
      juzListPresenter.quarters().toTypedArray()
    } else {
      context.resources.getStringArray(R.array.quarter_prefix_array)
    }
    if (quarters.isNotEmpty()) rows = buildRows(quarters)
  }

  /** The row of the hizb [page] is in, so it can be scrolled into view. */
  fun positionFor(page: Int): Int {
    val quarter = quarterPages.indexOfLast { it <= page }.coerceAtLeast(0)
    return (quarter / 4) * ROWS_PER_HIZB
  }

  private fun buildRows(quarters: Array<String>): List<QuranRow> {
    val elements = ArrayList<QuranRow>(HIZB_COUNT * ROWS_PER_HIZB)
    for (i in 0 until QUARTERS) {
      val pos = quranInfo.getQuarterByIndex(i)
      val page = quarterPages[i]
      val hizbNumber = QuranUtils.getLocalizedNumber(1 + i / 4)
      if (i % 4 == 0) {
        elements += QuranRow.Builder()
          .withType(QuranRow.HEADER)
          .withText(context.getString(R.string.hizb_description, hizbNumber))
          .withPage(page)
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
      if (i % 4 == 0) builder.withJuzOverlayText(hizbNumber)
      elements += builder.build()
    }
    return elements
  }

  private companion object {
    private const val HIZB_COUNT = 60
    private const val QUARTERS = HIZB_COUNT * 4
    private const val ROWS_PER_HIZB = 5
    private val ENTRY_TYPES = intArrayOf(
      JuzView.TYPE_JUZ, JuzView.TYPE_QUARTER,
      JuzView.TYPE_HALF, JuzView.TYPE_THREE_QUARTERS
    )
  }
}
