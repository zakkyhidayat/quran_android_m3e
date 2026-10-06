package com.quran.labs.androidquran

import android.app.SearchManager
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.SpannableString
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.quran.data.core.QuranInfo
import com.quran.data.model.SuraAyah
import com.quran.labs.androidquran.common.ui.core.QuranTheme
import com.quran.labs.androidquran.data.QuranDataProvider
import com.quran.labs.androidquran.data.QuranDisplayData
import com.quran.labs.androidquran.presenter.data.ReaderReadinessTracker
import com.quran.labs.androidquran.service.QuranDownloadService
import com.quran.labs.androidquran.service.util.DefaultDownloadReceiver
import com.quran.labs.androidquran.service.util.DefaultDownloadReceiver.SimpleDownloadListener
import com.quran.labs.androidquran.service.util.QuranDownloadNotifier
import com.quran.labs.androidquran.service.util.ServiceIntentHelper.getDownloadIntent
import com.quran.labs.androidquran.ui.TranslationManagerActivity
import com.quran.labs.androidquran.ui.compose.SearchResult
import com.quran.labs.androidquran.ui.compose.SearchScreen
import com.quran.labs.androidquran.ui.helpers.QuranNavigator
import com.quran.labs.androidquran.util.QuranFileUtils
import com.quran.labs.androidquran.util.QuranSettings
import com.quran.labs.androidquran.util.QuranUtils
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Activity for searching the Quran
 */
class SearchActivity : AppCompatActivity(), SimpleDownloadListener {
  private var downloadArabicSearchDb = false
  private var jumpToTranslation = true
  private var downloadReceiver: DefaultDownloadReceiver? = null

  // what the screen shows
  private var query by mutableStateOf("")
  private var summary by mutableStateOf<String?>(null)
  private var warning by mutableStateOf<String?>(null)
  private var actionLabel by mutableStateOf<String?>(null)
  private var results by mutableStateOf<List<SearchResult>>(emptyList())

  /**
   * Routes at most one search suggestion at a time.
   *
   * Replacing this job prevents an older [onNewIntent] call from opening after a newer intent.
   */
  private var viewIntentJob: Job? = null

  /** Only the latest search may fill in the results, so a slow earlier one can't overwrite it. */
  private var searchJob: Job? = null

  @Inject lateinit var quranNavigatorFactory: QuranNavigator.Factory
  private val quranNavigator by lazy { quranNavigatorFactory.create(this) }

  @Inject
  lateinit var quranInfo: QuranInfo

  @Inject
  lateinit var quranDisplayData: QuranDisplayData

  @Inject
  lateinit var quranFileUtils: QuranFileUtils

  @Inject
  lateinit var quranSettings: QuranSettings

  @Inject
  lateinit var readerReadinessTracker: ReaderReadinessTracker

  public override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    (application as QuranApplication)
      .applicationComponent.inject(this)

    setContent {
      QuranTheme {
        SearchScreen(
          query = query,
          summary = summary,
          warning = warning,
          actionLabel = actionLabel,
          results = results,
          onSearch = ::showResults,
          onAction = ::onActionClicked,
          onResultClick = { result ->
            cancelPendingViewIntentNavigation()
            jumpToResult(result.sura, result.ayah, jumpToTranslation = jumpToTranslation)
          },
          onBack = ::finish
        )
      }
    }
    handleIntent(intent)
  }

  private fun onActionClicked() {
    if (downloadArabicSearchDb) {
      downloadArabicSearchDb()
    } else {
      startActivity(Intent(applicationContext, TranslationManagerActivity::class.java))
      finish()
    }
  }

  public override fun onPause() {
    val receiver = downloadReceiver
    if (receiver != null) {
      receiver.setListener(null)
      LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
      downloadReceiver = null
    }
    super.onPause()
  }

  override fun onStop() {
    cancelPendingViewIntentNavigation()
    super.onStop()
  }

  private fun downloadArabicSearchDb() {
    if (downloadReceiver == null) {
      val receiver = DefaultDownloadReceiver(
        this, QuranDownloadService.DOWNLOAD_TYPE_ARABIC_SEARCH_DB
      )
      LocalBroadcastManager.getInstance(this).registerReceiver(
        receiver, IntentFilter(QuranDownloadNotifier.ProgressIntent.INTENT_NAME)
      )
      downloadReceiver = receiver
    }
    downloadReceiver?.setListener(this)

    val url = quranFileUtils.arabicSearchDatabaseUrl
    val notificationTitle = getString(R.string.search_data)
    val intent = getDownloadIntent(
      this, url,
      quranFileUtils.getQuranDatabaseDirectory().absolutePath,
      notificationTitle, SEARCH_INFO_DOWNLOAD_KEY,
      QuranDownloadService.DOWNLOAD_TYPE_ARABIC_SEARCH_DB
    )
    val extension = if (url.endsWith(".zip")) ".zip" else ""
    intent.putExtra(
      QuranDownloadService.EXTRA_OUTPUT_FILE_NAME,
      QuranDataProvider.QURAN_ARABIC_DATABASE + extension
    )
    startService(intent)
  }

  override fun handleDownloadSuccess() {
    warning = null
    actionLabel = null
    handleIntent(intent)
  }

  override fun handleDownloadFailure(errId: Int) {
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIntent(intent)
  }

  private fun handleIntent(intent: Intent?) {
    if (intent == null) {
      return
    }

    cancelPendingViewIntentNavigation()

    if (Intent.ACTION_SEARCH == intent.action) {
      val query = intent.getStringExtra(SearchManager.QUERY)
      showResults(query)
    } else if (Intent.ACTION_VIEW == intent.action) {
      val intentData = intent.data
      var query = intent.getStringExtra(SearchManager.USER_QUERY)
      if (query == null) {
        val extras = intent.extras
        if (extras != null) {
          // bug on ics where the above returns null
          // http://code.google.com/p/android/issues/detail?id=22978
          val q = extras.get(SearchManager.USER_QUERY)
          if (q is SpannableString) {
            query = q.toString()
          }
        }
      }

      val id = intentData?.lastPathSegment?.toIntOrNull() ?: return
      if (id == -1) {
        showResults(query)
        return
      }
      if (id !in 1..quranInfo.getNumberOfAyahsInQuran()) return

      val (sura, ayah) = quranInfo.getSuraAyahFromAyahId(id)

      val isArabicQuery = QuranUtils.doesStringContainArabic(query)
      viewIntentJob = lifecycleScope.launch {
        // This check can copy the database. Await it off main before deciding where to open.
        val openAsArabic = isArabicQuery && withContext(Dispatchers.IO) {
          quranFileUtils.hasArabicSearchDatabase()
        }

        jumpToResult(sura, ayah, jumpToTranslation = !openAsArabic)
        finish()
      }
    }
  }

  private fun cancelPendingViewIntentNavigation() {
    viewIntentJob?.cancel()
    viewIntentJob = null
  }

  /**
   * Opens an ayah in the reader.
   *
   * @param sura the one-based sura number to open.
   * @param ayah the one-based ayah number to highlight.
   * @param jumpToTranslation whether the reader should open its translation view.
   */
  private fun jumpToResult(sura: Int, ayah: Int, jumpToTranslation: Boolean) {
    if (canOpenReaderDirectly()) {
      quranNavigator.jumpTo(SuraAyah(sura, ayah), showTranslation = jumpToTranslation)
    } else {
      startActivity(Intent(this, QuranDataActivity::class.java))
      finish()
    }
  }

  private fun canOpenReaderDirectly(): Boolean {
    return quranSettings.haveMigratedLegacyBookmarksToMobileSync() &&
      readerReadinessTracker.isReady(quranSettings.pageType)
  }

  private fun showResults(searchQuery: String?) {
    val text = searchQuery.orEmpty()
    query = text
    searchJob?.cancel()
    searchJob = lifecycleScope.launch {
      val found = withContext(Dispatchers.IO) { search(text) }
      applyResults(text, found)
    }
  }

  /** Runs the query against the search provider. null means there was nothing to search. */
  private fun search(text: String): List<SearchResult>? {
    val cursor = contentResolver.query(
      QuranDataProvider.SEARCH_URI, null, null, arrayOf<String?>(text), null
    ) ?: return null

    return cursor.use {
      val rows = ArrayList<SearchResult>(it.count)
      while (it.moveToNext()) {
        val sura = it.getInt(1)
        val ayah = it.getInt(2)
        val page = quranInfo.getPageFromSuraAyah(sura, ayah)
        rows += SearchResult(
          sura = sura,
          ayah = ayah,
          html = it.getString(3).orEmpty(),
          location = getString(
            R.string.found_in_sura,
            sura,
            quranDisplayData.getSuraName(this, sura, false),
            ayah,
            page
          )
        )
      }
      rows
    }
  }

  private fun applyResults(text: String, found: List<SearchResult>?) {
    val containsArabic = QuranUtils.doesStringContainArabic(text)
    val showArabicWarning = containsArabic &&
      !quranFileUtils.hasTranslation(QuranDataProvider.QURAN_ARABIC_DATABASE)
    jumpToTranslation = !containsArabic || showArabicWarning

    if (showArabicWarning) {
      // Without the Arabic database, Arabic tafseer matches should open in translation view.
      warning = getString(R.string.no_arabic_search_available)
      actionLabel = getString(R.string.get_arabic_search_db)
      downloadArabicSearchDb = true
    } else {
      warning = null
      actionLabel = null
      downloadArabicSearchDb = false
    }

    if (found == null) {
      summary = getString(R.string.no_results, text)
      // null is returned either when the query length is less than 3 characters or when
      // there are no valid databases to search at all. in this case, if it's not an
      // Arabic search, show the "get translations" button.
      if (!containsArabic && text.length > 2) {
        actionLabel = getString(R.string.get_translations)
      }
      results = emptyList()
    } else {
      summary = resources.getQuantityString(
        R.plurals.search_results, found.size, text, found.size
      )
      results = found
    }
  }

  companion object {
    const val SEARCH_INFO_DOWNLOAD_KEY: String = "SEARCH_INFO_DOWNLOAD_KEY"
  }
}
