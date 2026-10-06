package com.quran.labs.androidquran.ui

import android.app.SearchManager
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AlertDialog.Builder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.quran.data.dao.RecentPagesDao
import com.quran.data.model.Page
import com.quran.data.model.SuraAyah
import com.quran.labs.androidquran.AboutUsActivity
import com.quran.labs.androidquran.BuildConfig
import com.quran.labs.androidquran.HelpActivity
import com.quran.labs.androidquran.QuranApplication
import com.quran.labs.androidquran.QuranPreferenceActivity
import com.quran.data.core.QuranInfo
import com.quran.data.dao.ReadingBookmarksDao
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.data.QuranDisplayData
import com.quran.labs.androidquran.presenter.data.JuzListPresenter
import com.quran.labs.androidquran.ui.compose.JuzListState
import com.quran.labs.androidquran.ui.compose.SuraListState
import com.quran.labs.androidquran.ui.helpers.QuranRowFactory
import com.quran.labs.androidquran.presenter.bookmark.BookmarkPresenter
import com.quran.labs.androidquran.ui.compose.BookmarksActions
import com.quran.labs.androidquran.ui.compose.BookmarksState
import com.quran.labs.androidquran.ui.helpers.BookmarkUIConverter
import com.quran.mobile.feature.sync.QuranSyncActivity
import com.quran.mobile.feature.sync.QuranSyncManager
import com.quran.labs.androidquran.common.ui.core.QuranTheme
import com.quran.labs.androidquran.ui.compose.HomeActions
import com.quran.labs.androidquran.ui.compose.HomeExtraItem
import com.quran.labs.androidquran.ui.compose.HomeScreen
import com.quran.labs.androidquran.SearchActivity
import com.quran.labs.androidquran.ShortcutsActivity
import com.quran.labs.androidquran.data.Constants
import com.quran.labs.androidquran.feature.reading.model.LatestPageTracker
import com.quran.labs.androidquran.presenter.data.QuranIndexEventLogger
import com.quran.labs.androidquran.presenter.translation.TranslationManagerPresenter
import com.quran.labs.androidquran.service.AudioService
import com.quran.labs.androidquran.ui.fragment.AddTagDialog
import com.quran.labs.androidquran.ui.fragment.AddTagDialog.Companion.newInstance
import com.quran.labs.androidquran.ui.fragment.JumpFragment
import com.quran.labs.androidquran.ui.fragment.TagBookmarkDialog
import com.quran.labs.androidquran.ui.fragment.TagBookmarkDialog.OnBookmarkTagsUpdateListener
import com.quran.labs.androidquran.ui.helpers.JumpDestination
import com.quran.labs.androidquran.ui.helpers.QuranNavigator
import com.quran.labs.androidquran.ui.helpers.QuranRow
import com.quran.labs.androidquran.util.AudioUtils
import com.quran.labs.androidquran.util.QuranSettings
import com.quran.labs.androidquran.util.QuranUtils
import com.quran.mobile.di.ExtraScreenProvider
import dev.zacsweers.metro.Inject
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * The home screen activity for the app. The Compose [HomeScreen] shows an app bar and three tabs:
 * the surah list, the juz list and the bookmarks.
 *
 * When this activity is created, it may run a background check to see if updated translations
 * are available, and if so, show a dialog asking the user if they want to download them.
 *
 * This activity is called from several places:
 *  * [com.quran.labs.androidquran.QuranDataActivity]
 *  * [ShortcutsActivity]
 */
class QuranActivity : AppCompatActivity(),
    OnBookmarkTagsUpdateListener,
    JumpDestination {
  private var upgradeDialog: AlertDialog? = null
  private var showedTranslationUpgradeDialog = false
  private var isRtl = false
  private var isPaused = false
  private val compositeDisposable = CompositeDisposable()
  private val latestPageFlow: Flow<Int> by lazy {
    combine(
      recentPagesDao.recentPagesFlow()
        .map { recentPages -> recentPages.firstOrNull()?.page ?: Constants.NO_PAGE },
      latestPageTracker.latestPage
    ) { persistedPage, latestPage ->
      latestPage
        ?.takeIf { it.pageType == settings.pageType }
        ?.page
        ?: persistedPage
    }
      .distinctUntilChanged()
  }

  suspend fun latestPage(): Int {
    return latestPageFlow.first()
  }

  private var backStackListener: FragmentManager.OnBackStackChangedListener? = null

  @Inject lateinit var quranNavigator: QuranNavigator

  @Inject
  lateinit var settings: QuranSettings
  @Inject
  lateinit var audioUtils: AudioUtils
  @Inject
  lateinit var recentPagesDao: RecentPagesDao
  @Inject
  lateinit var latestPageTracker: LatestPageTracker
  @Inject
  lateinit var translationManagerPresenter: TranslationManagerPresenter
  @Inject
  lateinit var quranIndexEventLogger: QuranIndexEventLogger
  @Inject
  lateinit var extraScreens: Set<@JvmSuppressWildcards ExtraScreenProvider>
  @Inject
  lateinit var bookmarkPresenter: BookmarkPresenter
  @Inject
  lateinit var bookmarkUIConverter: BookmarkUIConverter
  @Inject
  lateinit var syncManager: QuranSyncManager

  @Inject
  lateinit var quranInfo: QuranInfo
  @Inject
  lateinit var quranDisplayData: QuranDisplayData
  @Inject
  lateinit var readingBookmarksDao: ReadingBookmarksDao
  @Inject
  lateinit var quranRowFactory: QuranRowFactory
  @Inject
  lateinit var juzListPresenter: JuzListPresenter

  private val suraListState by lazy {
    SuraListState(this, quranInfo, quranDisplayData, settings, readingBookmarksDao, quranRowFactory)
  }
  private val juzListState by lazy {
    JuzListState(this, quranInfo, quranDisplayData, juzListPresenter)
  }
  private val bookmarksState by lazy {
    BookmarksState(applicationContext, bookmarkPresenter, bookmarkUIConverter)
  }

  public override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    val quranApp = application as QuranApplication
    quranApp.applicationComponent
      .activityComponentFactory()
      .generate(this)
      .quranActivityComponentFactory()
      .generate()
      .inject(this)

    registerBackPressedCallbacks()
    isRtl = isRtl()

    setContent {
      QuranTheme {
        HomeScreen(
          actions = homeActions(),
          suraState = suraListState,
          juzState = juzListState,
          bookmarks = bookmarksState,
          bookmarkActions = bookmarksActions(),
          syncManager = syncManager,
          latestPage = ::latestPage,
          onRowClick = ::jumpTo
        )
      }
    }

    if (savedInstanceState != null) {
      showedTranslationUpgradeDialog = savedInstanceState.getBoolean(
          SI_SHOWED_UPGRADE_DIALOG, false
      )
    }

    val intent = intent
    if (intent != null) {
      val extras = intent.extras
      if (extras != null) {
        if (extras.getBoolean(EXTRA_SHOW_TRANSLATION_UPGRADE, false)) {
          if (!showedTranslationUpgradeDialog) {
            showTranslationsUpgradeDialog()
          }
        }
      }
      if (ShortcutsActivity.ACTION_JUMP_TO_LATEST == intent.action) {
        jumpToLastPage()
      }
    }
    updateTranslationsListAsNeeded()
    quranIndexEventLogger.logAnalytics()
  }

  override fun onStart() {
    super.onStart()
    bookmarksState.bind()
  }

  override fun onStop() {
    bookmarksState.unbind()
    super.onStop()
  }

  public override fun onResume() {
    super.onResume()
    val isRtl = isRtl()
    if (isRtl != this.isRtl) {
      val i = intent
      finish()
      startActivity(i)
    } else {
      if (BuildConfig.AUDIO_ENABLED) {
        compositeDisposable.add(
            Completable.timer(500, MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                  try {
                    startService(
                      audioUtils.getAudioIntent(this@QuranActivity, AudioService.ACTION_STOP)
                    )
                  } catch (_: IllegalStateException) {
                    // do nothing, we might be in the background
                    // onPause should have stopped us from needing this, but it sometimes happens
                  }
                }
        )
      }
    }
    isPaused = false
  }

  override fun onPause() {
    compositeDisposable.clear()
    isPaused = true
    super.onPause()
  }

  override fun onDestroy() {
    // only set to handle Android Q memory leaks
    backStackListener?.let {
      supportFragmentManager.removeOnBackStackChangedListener(it)
    }
    super.onDestroy()
  }

  // on back pressed, these are run in reverse order of registration
  private fun registerBackPressedCallbacks() {
    // this block works around a memory leak in Android Q
    // https://issuetracker.google.com/issues/139738913
    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && isTaskRoot) {
      val enabled = (supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.backStackEntryCount ?: 0) == 0 &&
          supportFragmentManager.backStackEntryCount == 0
      val callback = object : OnBackPressedCallback(enabled) {
        override fun handleOnBackPressed() {
          finishAfterTransition()
        }
      }
      onBackPressedDispatcher.addCallback(this, callback)

      val listener = FragmentManager.OnBackStackChangedListener {
        callback.isEnabled =
          (supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.backStackEntryCount
            ?: 0) == 0 &&
              supportFragmentManager.backStackEntryCount == 0
      }
      backStackListener = listener
      supportFragmentManager.addOnBackStackChangedListener(listener)
    }
  }

  private fun isRtl(): Boolean {
    return QuranUtils.isRtl()
  }

  private fun bookmarksActions() = BookmarksActions(
    onJumpTo = ::jumpTo,
    onOpenCollection = { row ->
      row.tagId?.let { startActivity(BookmarkListActivity.collectionIntent(this, it, row.text)) }
    },
    onOpenHighlightColor = { startActivity(BookmarkListActivity.highlightsIntent(this, it)) },
    onAddTag = ::addTag,
    onEditTag = ::editTag,
    onTagBookmarks = ::tagBookmarks
  )

  private fun homeActions() = HomeActions(
    onSearch = { query ->
      startActivity(
        Intent(this, SearchActivity::class.java)
          .setAction(Intent.ACTION_SEARCH)
          .putExtra(SearchManager.QUERY, query)
      )
    },
    onSignIn = { startActivity(Intent(this, QuranSyncActivity::class.java)) },
    onLastPage = ::jumpToLastPage,
    onJumpToPage = ::gotoPageDialog,
    onSettings = { startActivity(Intent(this, QuranPreferenceActivity::class.java)) },
    onHelp = { startActivity(Intent(this, HelpActivity::class.java)) },
    onAbout = { startActivity(Intent(this, AboutUsActivity::class.java)) },
    onOtherApps = {
      val intent = Intent(Intent.ACTION_VIEW)
      intent.data = "market://search?q=pub:quran.com".toUri()
      if (packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) == null) {
        intent.data = "https://play.google.com/store/search?q=pub:quran.com".toUri()
      }
      startActivity(intent)
    },
    extraItems = extraScreens
      .sortedBy { it.order }
      .map { screen -> HomeExtraItem(screen.titleResId) { screen.onClick(this) } }
  )

  override fun onSaveInstanceState(outState: Bundle) {
    outState.putBoolean(
        SI_SHOWED_UPGRADE_DIALOG,
        showedTranslationUpgradeDialog
    )
    super.onSaveInstanceState(outState)
  }

  private fun jumpToLastPage() {
    lifecycleScope.launch {
      val recentPage = latestPage()
      jumpTo(
        if (recentPage == Constants.NO_PAGE) 1 else recentPage
      )
    }
  }

  private fun updateTranslationsListAsNeeded() {
    if (!updatedTranslations) {
      translationManagerPresenter.checkForUpdates()
      updatedTranslations = true
    }
  }

  private fun showTranslationsUpgradeDialog() {
    showedTranslationUpgradeDialog = true

    val builder = Builder(this)
    builder.setMessage(R.string.translation_updates_available)
    builder.setCancelable(false)
    builder.setPositiveButton(R.string.translation_dialog_yes) { dialog: DialogInterface, _: Int ->
      dialog.dismiss()
      upgradeDialog = null
      launchTranslationActivity()
    }

    builder.setNegativeButton(R.string.translation_dialog_later) { dialog: DialogInterface, _: Int ->
      dialog.dismiss()
      upgradeDialog = null
      // pretend we don't have updated translations.  we'll
      // check again after 10 days.
      settings.setHaveUpdatedTranslations(false)
    }

    val dialog = builder.create()
    dialog.show()
    upgradeDialog = dialog
  }

  private fun launchTranslationActivity() {
    val i = Intent(this, TranslationManagerActivity::class.java)
    startActivity(i)
  }

  override fun jumpTo(page: Int) {
    quranNavigator.jumpTo(Page(page), showTranslation = settings.wasShowingTranslation)
  }

  override fun jumpToAndHighlight(page: Int, sura: Int, ayah: Int) {
    quranNavigator.jumpTo(SuraAyah(sura, ayah), showTranslation = settings.wasShowingTranslation)
  }

  fun jumpTo(row: QuranRow) {
    val location = if (row.isAyahBookmark || row.isHighlightedAyah) {
      SuraAyah(row.sura, row.ayah)
    } else {
      Page(row.page)
    }
    quranNavigator.jumpTo(location, row.readingBookmarkType, settings.wasShowingTranslation)
  }

  private fun gotoPageDialog() {
    if (!isPaused) {
      val fm = supportFragmentManager
      val jumpDialog = JumpFragment()
      jumpDialog.show(fm, JumpFragment.TAG)
    }
  }

  fun addTag() {
    if (!isPaused) {
      val fm = supportFragmentManager
      val addTagDialog = AddTagDialog()
      addTagDialog.show(fm, AddTagDialog.TAG)
    }
  }

  fun editTag(id: String, name: String?) {
    if (!isPaused) {
      val fm = supportFragmentManager
      val addTagDialog = newInstance(id, name!!)
      addTagDialog.show(fm, AddTagDialog.TAG)
    }
  }

  fun tagBookmarks(ids: Array<String>?) {
    if (ids != null && ids.size == 1) {
      tagBookmark(ids[0])
      return
    }

    if (!isPaused) {
      val fm = supportFragmentManager
      val tagBookmarkDialog = TagBookmarkDialog.newInstance(ids)
      tagBookmarkDialog.show(fm, TagBookmarkDialog.TAG)
    }
  }

  private fun tagBookmark(id: String) {
    if (!isPaused) {
      val fm = supportFragmentManager
      val tagBookmarkDialog = TagBookmarkDialog.newInstance(id)
      tagBookmarkDialog.show(fm, TagBookmarkDialog.TAG)
    }
  }

  override fun onAddTagSelected() {
    val fm = supportFragmentManager
    val dialog = AddTagDialog()
    dialog.show(fm, AddTagDialog.TAG)
  }

  companion object {
    const val EXTRA_SHOW_TRANSLATION_UPGRADE = "transUp"
    private const val SI_SHOWED_UPGRADE_DIALOG = "si_showed_dialog"
    private var updatedTranslations = false
  }
}
