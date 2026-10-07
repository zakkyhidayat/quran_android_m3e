package com.quran.labs.androidquran.ui

import android.Manifest
import android.app.ProgressDialog
import android.app.SearchManager
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.preference.PreferenceManager
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.quran.labs.androidquran.common.ui.core.QuranTheme
import com.quran.labs.androidquran.ui.compose.ReaderBarActions
import com.quran.labs.androidquran.ui.compose.ReaderBarState
import com.quran.labs.androidquran.ui.compose.AyahBadge
import com.quran.labs.androidquran.ui.compose.MarkerPill
import com.quran.labs.androidquran.ui.compose.ReaderModeBar
import com.quran.labs.androidquran.ui.compose.ReaderView
import com.quran.labs.androidquran.ui.compose.ReaderTopBar
import com.quran.labs.androidquran.ui.compose.ReaderTranslationItem
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager.widget.NonRestoringViewPager
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.google.android.material.R as MaterialR
import com.google.android.material.color.MaterialColors
import com.quran.data.core.QuranInfo
import com.quran.data.dao.BookmarksDao
import com.quran.data.model.Page
import com.quran.data.model.QuranText
import com.quran.data.model.SuraAyah
import com.quran.data.model.bookmark.ReadingBookmarkTarget
import com.quran.data.model.selection.AyahSelection
import com.quran.data.model.selection.AyahSelection.AyahRange
import com.quran.data.model.selection.SelectionIndicator
import com.quran.data.model.selection.endSuraAyah
import com.quran.data.model.selection.selectionIndicator
import com.quran.data.model.selection.startSuraAyah
import com.quran.data.model.selection.withXScroll
import com.quran.labs.androidquran.BuildConfig
import com.quran.labs.androidquran.HelpActivity
import com.quran.labs.androidquran.QuranApplication
import com.quran.labs.androidquran.QuranPreferenceActivity
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.SearchActivity
import com.quran.labs.androidquran.common.QuranAyahInfo
import com.quran.labs.androidquran.common.audio.model.QariItem
import com.quran.labs.androidquran.common.audio.model.QariItem.Companion.fromQari
import com.quran.labs.androidquran.common.audio.model.playback.AudioRequest
import com.quran.labs.androidquran.common.audio.repository.AudioStatusRepository
import com.quran.labs.androidquran.common.audio.repository.CurrentQariManager
import com.quran.labs.androidquran.data.Constants
import com.quran.labs.androidquran.data.QuranDataProvider
import com.quran.labs.androidquran.data.QuranDisplayData
import com.quran.labs.androidquran.di.component.activity.PagerActivityComponent
import com.quran.labs.androidquran.feature.reading.bridge.AudioStatusRepositoryBridge
import com.quran.labs.androidquran.feature.reading.bridge.DownloadBridge
import com.quran.labs.androidquran.feature.reading.bridge.ReadingEventPresenterBridge
import com.quran.labs.androidquran.feature.reading.presenter.AudioPresenter
import com.quran.labs.androidquran.feature.reading.presenter.AudioPresenterScreen
import com.quran.labs.androidquran.feature.reading.presenter.ReadingBookmarkChange
import com.quran.labs.androidquran.feature.reading.presenter.ReadingBookmarkPresenter
import com.quran.labs.androidquran.feature.reading.presenter.RecentPagePresenter
import com.quran.labs.androidquran.feature.reading.presenter.recitation.PagerActivityRecitationPresenter
import com.quran.labs.androidquran.model.translation.ArabicDatabaseUtils
import com.quran.labs.androidquran.presenter.data.QuranEventLogger
import com.quran.labs.androidquran.presenter.translationlist.TranslationListPresenter
import com.quran.labs.androidquran.service.AudioService
import com.quran.labs.androidquran.service.QuranDownloadService
import com.quran.labs.androidquran.service.util.PermissionUtil.buildPostPermissionDialog
import com.quran.labs.androidquran.service.util.PermissionUtil.canRequestPostNotificationPermission
import com.quran.labs.androidquran.service.util.PermissionUtil.havePostNotificationPermission
import com.quran.labs.androidquran.service.util.ServiceIntentHelper.getDownloadIntent
import com.quran.labs.androidquran.ui.fragment.AddTagDialog
import com.quran.labs.androidquran.ui.fragment.JumpFragment
import com.quran.labs.androidquran.ui.fragment.TabletFragment
import com.quran.labs.androidquran.ui.fragment.TagBookmarkDialog.OnBookmarkTagsUpdateListener
import com.quran.labs.androidquran.ui.fragment.TranslationFragment
import com.quran.labs.androidquran.ui.helpers.AyahSelectedListener
import com.quran.labs.androidquran.ui.helpers.AyahTracker
import com.quran.labs.androidquran.ui.helpers.JumpDestination
import com.quran.labs.androidquran.ui.helpers.QuranDisplayHelper
import com.quran.labs.androidquran.ui.helpers.QuranNavigator
import com.quran.labs.androidquran.ui.helpers.QuranPage
import com.quran.labs.androidquran.ui.helpers.QuranPageAdapter
import com.quran.labs.androidquran.ui.fragment.AyahTranslationFragment
import com.quran.labs.androidquran.ui.helpers.SlidingPagerAdapter
import com.quran.labs.androidquran.ui.listener.AudioBarListener
import com.quran.labs.androidquran.ui.readingbookmark.createReadingBookmarkToastView
import com.quran.labs.androidquran.ui.util.ToastCompat.makeText
import com.quran.labs.androidquran.ui.util.TranslationsSpinnerAdapter
import com.quran.labs.androidquran.util.AudioUtils
import com.quran.labs.androidquran.util.OrientationLockUtils
import com.quran.labs.androidquran.util.QuranAppUtils
import com.quran.labs.androidquran.util.QuranFileUtils
import com.quran.labs.androidquran.util.QuranScreenInfo
import com.quran.labs.androidquran.util.QuranSettings
import com.quran.labs.androidquran.util.QuranUtils
import com.quran.labs.androidquran.util.ShareUtil
import com.quran.labs.androidquran.view.IconPageIndicator
import com.quran.labs.androidquran.view.QuranSpinner
import com.quran.labs.androidquran.view.SlidingUpPanelLayout
import com.quran.mobile.common.download.DownloadInfoStreams
import com.quran.mobile.di.AyahActionFragmentProvider
import com.quran.mobile.di.QuranReadingActivityComponent
import com.quran.mobile.di.QuranReadingActivityComponentProvider
import com.quran.mobile.di.QuranReadingPageComponent
import com.quran.mobile.di.QuranReadingPageComponentProvider
import com.quran.mobile.feature.audiobar.AudioBarInjector
import com.quran.mobile.feature.audiobar.AudioBarWrapper
import com.quran.mobile.feature.audiobar.presenter.AudioBarEventRepository
import com.quran.mobile.feature.ayahbookmark.AyahBookmarkWrapper
import com.quran.mobile.feature.ayahbookmark.di.AyahBookmarkWrapperInjector
import com.quran.mobile.feature.ayahbookmark.readingbookmark.ReadingBookmarkSheetWrapper
import com.quran.mobile.feature.ayahbookmark.readingbookmark.di.ReadingBookmarkSheetWrapperInjector
import com.quran.mobile.feature.qarilist.QariListWrapper
import com.quran.mobile.feature.qarilist.di.QariListWrapperInjector
import com.quran.mobile.translation.model.LocalTranslation
import com.quran.page.common.data.QuranNaming
import com.quran.page.common.factory.PageViewFactoryProvider
import com.quran.page.common.toolbar.AyahToolBar
import com.quran.page.common.toolbar.di.AyahToolBarInjector
import com.quran.reading.common.ReadingEventPresenter
import dev.zacsweers.metro.Inject
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.observers.DisposableSingleObserver
import java.lang.ref.WeakReference
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Activity that displays the Quran (in Arabic or translation mode).
 *
 *
 * Essentially, this activity consists of a [ViewPager] of Quran pages (using [ ]).
 * [AudioService] is used to handle playing audio, and this is synced with
 * the display of the Quran.
 */
class PagerActivity : AppCompatActivity(), AudioBarListener, OnBookmarkTagsUpdateListener,
  AyahSelectedListener, JumpDestination, QuranReadingActivityComponentProvider,
  QuranReadingPageComponentProvider, AyahToolBarInjector, QariListWrapperInjector,
  AudioBarInjector, AyahBookmarkWrapperInjector, ReadingBookmarkSheetWrapperInjector,
  ActivityCompat.OnRequestPermissionsResultCallback, AudioPresenterScreen,
  ReadingBookmarkPresenter.Screen {
  private var lastPopupTime: Long = 0
  private val ayahLabel = androidx.compose.runtime.mutableStateOf("")
  private var markerJob: kotlinx.coroutines.Job? = null
  private var shouldReconnect = false
  private var showingTranslation = false
  private var needsPermissionToDownloadOver3g = true
  private var promptDialog: AlertDialog? = null
  private var isDualPages = false
  private var promptedForExtraDownload = false
  private var progressDialog: ProgressDialog? = null
  private var isFoldableDeviceOpenAndVertical = false

  private val readerBar = ReaderBarState()
  private var isCurrentPageReadingBookmarked = false
  private var readingBookmarkToastView: View? = null

  private var translationNames: Array<String> = emptyArray()
  private var translations: List<LocalTranslation>? = null
  private var activeTranslationsFilesNames: Set<String?>? = null

  private lateinit var audioStatusBar: AudioBarWrapper
  private lateinit var viewPager: ViewPager
  private lateinit var pagerAdapter: QuranPageAdapter
  private lateinit var ayahToolBar: AyahToolBar

  /** Where the ayah window's translation picker goes. */
  val ayahToolbarHeader: android.widget.FrameLayout get() = ayahToolBar.headerContainer
  private lateinit var slidingPanel: SlidingUpPanelLayout
  private lateinit var slidingPager: ViewPager
  private lateinit var slidingPagerAdapter: SlidingPagerAdapter
  private lateinit var overlay: FrameLayout

  private var requestPermissionLauncher: ActivityResultLauncher<String>? = null

  private var isSplitScreen = false

  private lateinit var onClearAyahModeBackCallback: OnBackPressedCallback
  private lateinit var onShowingTranslationBackCallback: OnBackPressedCallback
  private lateinit var onEndSessionBackCallback: OnBackPressedCallback

  private var lastSelectedTranslationAyah: QuranAyahInfo? = null
  private var lastActivatedLocalTranslations: Array<LocalTranslation> = emptyArray()

  @Inject lateinit var quranNavigator: QuranNavigator

  @Inject lateinit var bookmarksDao: BookmarksDao
  @Inject lateinit var recentPagePresenter: RecentPagePresenter
  @Inject lateinit var readingBookmarkPresenter: ReadingBookmarkPresenter
  @Inject lateinit var quranSettings: QuranSettings
  @Inject lateinit var quranNaming: QuranNaming
  @Inject lateinit var quranScreenInfo: QuranScreenInfo
  @Inject lateinit var arabicDatabaseUtils: ArabicDatabaseUtils
  @Inject lateinit var quranAppUtils: QuranAppUtils
  @Inject lateinit var shareUtil: ShareUtil
  @Inject lateinit var audioUtils: AudioUtils
  @Inject lateinit var quranDisplayData: QuranDisplayData
  @Inject lateinit var quranInfo: QuranInfo
  @Inject lateinit var quranFileUtils: QuranFileUtils
  @Inject lateinit var audioPresenter: AudioPresenter
  @Inject lateinit var quranEventLogger: QuranEventLogger
  @Inject lateinit var audioStatusRepository: AudioStatusRepository
  @Inject lateinit var readingEventPresenter: ReadingEventPresenter
  @Inject lateinit var pageProviderFactoryProvider: PageViewFactoryProvider
  @Inject lateinit var additionalAyahPanels: Set<@JvmSuppressWildcards AyahActionFragmentProvider>
  @Inject lateinit var pagerActivityRecitationPresenter: PagerActivityRecitationPresenter
  @Inject lateinit var translationListPresenter: TranslationListPresenter
  @Inject lateinit var audioBarEventRepository: AudioBarEventRepository
  @Inject lateinit var downloadInfoStreams: DownloadInfoStreams
  @Inject lateinit var qariManager: CurrentQariManager
  @Inject lateinit var downloadBridge: DownloadBridge

  private lateinit var audioStatusRepositoryBridge: AudioStatusRepositoryBridge
  private lateinit var readingEventPresenterBridge: ReadingEventPresenterBridge
  private lateinit var windowInsetsController: WindowInsetsControllerCompat

  private var translationJob: Job? = null
  private lateinit var compositeDisposable: CompositeDisposable
  private val foregroundDisposable = CompositeDisposable()
  private val scope = MainScope()

  val pagerActivityComponent: PagerActivityComponent by lazy {
    (application as QuranApplication)
      .applicationComponent
      .activityComponentFactory()
      .generate(this)
      .pagerActivityComponentFactory()
      .generate(this)
  }

  public override fun onCreate(savedInstanceState: Bundle?) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    super.onCreate(savedInstanceState)

    // field injection
    pagerActivityComponent.inject(this)

    isFoldableDeviceOpenAndVertical =
      savedInstanceState?.getBoolean(LAST_FOLDING_STATE, isFoldableDeviceOpenAndVertical)
        ?: isFoldableDeviceOpenAndVertical

    lifecycleScope.launch {
      lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        WindowInfoTracker.getOrCreate(this@PagerActivity)
          .windowLayoutInfo(this@PagerActivity)
          .map { it.displayFeatures }
          .collectLatest {
            val foldingFeatures = it.filterIsInstance<FoldingFeature>().firstOrNull()
            if (foldingFeatures != null) {
              val localState =
                foldingFeatures.orientation == FoldingFeature.Orientation.VERTICAL &&
                    (foldingFeatures.state == FoldingFeature.State.FLAT || foldingFeatures.state == FoldingFeature.State.HALF_OPENED)
              if (isFoldableDeviceOpenAndVertical != localState) {
                isFoldableDeviceOpenAndVertical = localState
                updateDualPageMode()
              }
            } else if (isFoldableDeviceOpenAndVertical) {
              // this else case suggests that the device is not open and vertical, otherwise
              // we'd have some information given via the folding features.
              isFoldableDeviceOpenAndVertical = false
              updateDualPageMode()
            }
          }
      }
    }

    setContentView(R.layout.quran_page_activity_slider)

    val lightStatusBar = resources.getBoolean(R.bool.light_navigation_bar)
    windowInsetsController = WindowInsetsControllerCompat(
      window, findViewById<ViewGroup>(R.id.sliding_panel)
    ).apply {
      isAppearanceLightStatusBars = lightStatusBar
      isAppearanceLightNavigationBars = lightStatusBar
    }
    registerBackPressedCallbacks()
    initialize(savedInstanceState)
    requestPermissionLauncher =
      registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean? ->
        audioPresenter.onPostNotificationsPermissionResponse(
          isGranted!!
        )
      }

    // read the list of translations
    requestTranslationsList()

    downloadBridge.subscribeToDownloads {
      onDownloadSuccess()
    }

  }

  private fun updateDualPageMode() {
    val lastIsDualPages = isDualPages
    val nextIsDualPages = QuranUtils.isDualPages(this, quranScreenInfo, isFoldableDeviceOpenAndVertical)
    if (lastIsDualPages != nextIsDualPages) {
      // due to the conversion after setting the adapter, we need this page to be
      // the page _before_ we switched from dual to single or vice versa. otherwise,
      // we end up double-converting the page.
      //
      // might be worth trying later to undo this and just remove the conversion below.
      val page = currentPage
      isDualPages = nextIsDualPages

      pagerAdapter = QuranPageAdapter(
        supportFragmentManager,
        isDualPages,
        showingTranslation,
        quranInfo,
        isSplitScreen,
        pageProviderFactoryProvider.providePageViewFactory(quranSettings.pageType)
      )
      viewPager.adapter = pagerAdapter
      // when going from two page per screen to one or vice versa, we adjust the page number,
      // such that the first page is always selected.
      val curPage = if (isDualPageVisible) {
        quranInfo.mapSinglePageToDualPage(page)
      } else {
        quranInfo.mapDualPageToSinglePage(page)
      }

      val pageIndex = quranInfo.getPositionFromPage(curPage, isDualPageVisible)
      viewPager.setCurrentItem(pageIndex)
    }
  }

  private fun initialize(savedInstanceState: Bundle?) {
    var shouldAdjustPageNumber = false
    isDualPages = QuranUtils.isDualPages(this, quranScreenInfo, isFoldableDeviceOpenAndVertical)
    isSplitScreen = quranSettings.isQuranSplitWithTranslation
    audioStatusRepositoryBridge = AudioStatusRepositoryBridge(
      audioStatusRepository,
      audioBarEventRepository,
      { suraAyah: SuraAyah? ->
        onAudioPlaybackAyahChanged(suraAyah)
      },
      this,
      pagerActivityRecitationPresenter
    )
    readingEventPresenterBridge = ReadingEventPresenterBridge(
      readingEventPresenter,
      {
        onPageClicked()
      },
      handleSelection = { ayahSelection: AyahSelection ->
        onAyahSelectionChanged(ayahSelection)
      }
    )

    // remove the window background to avoid overdraw. note that, per Romain's blog, this is
    // acceptable (as long as we don't set the background color to null in the theme, since
    // that is used to generate preview windows).
    window.setBackgroundDrawable(null)

    var page = -1
    if (savedInstanceState != null) {
      Timber.d("non-null saved instance state!")
      page = savedInstanceState.getInt(LAST_READ_PAGE, -1)
      showingTranslation = savedInstanceState
        .getBoolean(LAST_READING_MODE_IS_TRANSLATION, false)
      val lastWasDualPages = savedInstanceState.getBoolean(LAST_WAS_DUAL_PAGES, isDualPages)
      shouldAdjustPageNumber = (lastWasDualPages != isDualPages)
    } else {
      val intent = intent
      val extras = intent.extras
      if (extras != null) {
        page = extras.getInt("page", Constants.PAGES_FIRST)
        showingTranslation =
          extras.getBoolean(EXTRA_JUMP_TO_TRANSLATION, showingTranslation)
        val highlightedSura = extras.getInt(EXTRA_HIGHLIGHT_SURA, -1)
        val highlightedAyah = extras.getInt(EXTRA_HIGHLIGHT_AYAH, -1)

        if (highlightedSura > -1 && highlightedAyah > -1) {
          readingEventPresenterBridge.setSelection(
            highlightedSura,
            highlightedAyah,
            true
          )
        }
      }
    }
    onShowingTranslationBackCallback.isEnabled = showingTranslation

    compositeDisposable = CompositeDisposable()

    audioStatusBar = findViewById(R.id.audio_area)

    overlay = findViewById(R.id.overlay)

    setupReaderBar()

    initAyahActionPanel()

    if (showingTranslation && translationNames.isNotEmpty()) {
      updateActionBarSpinner()
    } else {
      updateActionBarTitle(page)
    }

    lastPopupTime = System.currentTimeMillis()
    pagerAdapter = QuranPageAdapter(
      supportFragmentManager,
      isDualPages,
      showingTranslation,
      quranInfo,
      isSplitScreen,
      pageProviderFactoryProvider.providePageViewFactory(quranSettings.pageType)
    )
    ayahToolBar = findViewById(R.id.ayah_toolbar)
    ayahToolBar.flavor = BuildConfig.FLAVOR
    if (!BuildConfig.AUDIO_ENABLED) {
      ayahToolBar.setMenuItemVisibility(
        com.quran.labs.androidquran.common.toolbar.R.id.cab_play_from_here, false
      )
    }
    // the floating ayah window wears the theme's colors and carries the translation
    ayahToolBar.applyColors(
      MaterialColors.getColor(ayahToolBar, com.google.android.material.R.attr.colorSurfaceContainerHigh),
      MaterialColors.getColor(ayahToolBar, com.google.android.material.R.attr.colorOnSurface),
      MaterialColors.getColor(ayahToolBar, androidx.appcompat.R.attr.colorPrimary)
    )
    // when the page itself is the translation, the window needs no second copy of it
    ayahToolBar.contentEnabled = { !showingTranslation }
    if (supportFragmentManager.findFragmentById(ayahToolBar.contentContainer.id) == null) {
      supportFragmentManager.beginTransaction()
        .replace(ayahToolBar.contentContainer.id, AyahTranslationFragment())
        .commit()
    }
    ayahToolBar.dock = quranSettings.ayahWindowDock
    ayahToolbarHeader.addView(
      ComposeView(this).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
          QuranTheme {
            val label = ayahLabel.value
            if (label.isNotEmpty()) AyahBadge(label)
          }
        }
      },
      FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
        Gravity.START or Gravity.CENTER_VERTICAL
      )
    )
    ayahToolBar.onDockChanged = { quranSettings.ayahWindowDock = it }
    ayahToolBar.longPressLambda = { charSequence: CharSequence? ->
      makeText(this@PagerActivity, charSequence!!, Toast.LENGTH_SHORT).show()
    }

    ViewCompat.setOnApplyWindowInsetsListener(ayahToolBar) { view, windowInsets ->
      val insets = windowInsets.getInsets(
        WindowInsetsCompat.Type.statusBars() or
            WindowInsetsCompat.Type.displayCutout() or
            WindowInsetsCompat.Type.navigationBars()
      )
      // the page area starts under the top bar, so the status bar is already cleared
      // the page area already clears the system bars
      ayahToolBar.insets = androidx.core.graphics.Insets.NONE
      windowInsets
    }

    val nonRestoringViewPager = findViewById<NonRestoringViewPager>(R.id.quran_pager)
    nonRestoringViewPager.setIsDualPagesInLandscape(
      QuranUtils.isDualPagesInLandscape(this, quranScreenInfo)
    )

    viewPager = nonRestoringViewPager
    viewPager.setAdapter(pagerAdapter)

    ayahToolBar.setOnItemSelectedListener(AyahMenuItemSelectionHandler())
    val onPageChangeListener: OnPageChangeListener = object : OnPageChangeListener {
      override fun onPageScrollStateChanged(state: Int) {
      }

      override fun onPageScrolled(
        position: Int,
        positionOffset: Float,
        positionOffsetPixels: Int
      ) {
        val currentSelection = readingEventPresenter.currentAyahSelection()
        val selectionIndicator =
          currentSelection.selectionIndicator()
        val suraAyah = currentSelection.startSuraAyah()
        if (selectionIndicator !== SelectionIndicator.None && selectionIndicator !== SelectionIndicator.ScrollOnly && suraAyah != null) {
          val startPage = quranInfo.getPageFromSuraAyah(suraAyah.sura, suraAyah.ayah)
          val barPos = quranInfo.getPositionFromPage(startPage, isDualPageVisible)
          if (position == barPos) {
            // Swiping to next ViewPager page (i.e. prev quran page)
            val updatedSelectionIndicator =
              selectionIndicator.withXScroll(-positionOffsetPixels.toFloat())
            readingEventPresenterBridge.withSelectionIndicator(
              updatedSelectionIndicator
            )
          } else if (position == barPos - 1 || position == barPos + 1) {
            // Swiping to previous or next ViewPager page (i.e. next or previous quran page)
            val updatedSelectionIndicator =
              selectionIndicator.withXScroll((viewPager.width - positionOffsetPixels).toFloat())
            readingEventPresenterBridge.withSelectionIndicator(
              updatedSelectionIndicator
            )
          } else {
            readingEventPresenterBridge.clearSelectedAyah()
          }
        }
      }

      override fun onPageSelected(position: Int) {
        Timber.d("onPageSelected(): %d", position)
        val page = quranInfo.getPageFromPosition(position, isDualPageVisible)

        if (quranSettings.shouldDisplayMarkerPopup()) {
          showMarkerPopup(page)
          if (isDualPages) {
            showMarkerPopup(page - 1)
          }
        }

        if (!showingTranslation) {
          updateActionBarTitle(page)
        } else {
          refreshActionBarSpinner()
        }

        // If we're more than 1 page away from ayah selection end ayah mode
        val suraAyah: SuraAyah? = selectionStart
        if (suraAyah != null) {
          val startPage = quranInfo.getPageFromSuraAyah(suraAyah.sura, suraAyah.ayah)
          val ayahPos = quranInfo.getPositionFromPage(startPage, isDualPageVisible)
          if (abs((ayahPos - position).toDouble()) > 1) {
            endAyahMode()
          }
        }
      }
    }
    viewPager.addOnPageChangeListener(onPageChangeListener)

    audioStatusBar.visibility = if (BuildConfig.AUDIO_ENABLED) View.VISIBLE else View.GONE
    if (shouldAdjustPageNumber) {
      // when going from two page per screen to one or vice versa, we adjust the page number,
      // such that the first page is always selected.
      val curPage = if (isDualPageVisible) {
        quranInfo.mapSinglePageToDualPage(page)
      } else {
        quranInfo.mapDualPageToSinglePage(page)
      }
      page = curPage
    }

    val pageIndex = quranInfo.getPositionFromPage(page, isDualPageVisible)
    viewPager.setCurrentItem(pageIndex)
    if (page == 0) {
      onPageChangeListener.onPageSelected(0)
    }

    // just got created, need to reconnect to service
    shouldReconnect = true

    // enforce orientation lock
    if (quranSettings.isLockOrientation &&
      OrientationLockUtils.isOrientationLockSupported(resources.configuration)
    ) {
      val current = resources.configuration.orientation
      if (quranSettings.isLandscapeOrientation) {
        if (current == Configuration.ORIENTATION_PORTRAIT) {
          requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
          return
        }
      } else if (current == Configuration.ORIENTATION_LANDSCAPE) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        return
      }
    }

    quranEventLogger.logAnalytics(isDualPages, showingTranslation, isSplitScreen)

    // Setup recitation (if enabled)
    pagerActivityRecitationPresenter.bind(
      this, PagerActivityRecitationPresenter.Bridge(
        { isDualPageVisible },
        { currentPage },
        { ayahToolBar },
        { ayah: SuraAyah -> ensurePage(ayah.sura, ayah.ayah) },
        { sliderPage: Int -> showSlider(slidingPagerAdapter.getPagePosition(sliderPage)) }
      ))
  }

  private fun registerBackPressedCallbacks() {
    val isSessionEnabled = false
    onEndSessionBackCallback = object : OnBackPressedCallback(isSessionEnabled) {
      override fun handleOnBackPressed() {
        onSessionEnd()
      }
    }
    onBackPressedDispatcher.addCallback(this, onEndSessionBackCallback)

    onShowingTranslationBackCallback = object : OnBackPressedCallback(showingTranslation) {
      override fun handleOnBackPressed() {
        switchToQuran()
      }
    }
    onBackPressedDispatcher.addCallback(this, onShowingTranslationBackCallback)

    onClearAyahModeBackCallback = object : OnBackPressedCallback(selectionStart != null) {
      override fun handleOnBackPressed() {
        endAyahMode()
      }
    }
    onBackPressedDispatcher.addCallback(this, onClearAyahModeBackCallback)
  }

  override fun onRequestPermissionsResult(
    requestCode: Int, permissions: Array<String>, grantResults: IntArray
  ) {
    pagerActivityRecitationPresenter.onPermissionsResult(requestCode, grantResults)
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
  }

  private val isDualPageVisible: Boolean
    get() = isDualPages && !(isSplitScreen && showingTranslation)

  private fun shouldUpdatePageNumber(): Boolean {
    return isDualPages && isSplitScreen
  }

  private val currentPageFlow: Flow<Int> =
    callbackFlow {
      val pageChangedListener: OnPageChangeListener =
        object : SimpleOnPageChangeListener() {
          override fun onPageSelected(position: Int) {
            val page = quranInfo.getPageFromPosition(position, isDualPageVisible)
            trySend(page)
          }
        }
      viewPager.addOnPageChangeListener(pageChangedListener)
      awaitClose { viewPager.removeOnPageChangeListener(pageChangedListener) }
    }
      .onStart { emit(currentPage) }
      .buffer(onBufferOverflow = BufferOverflow.DROP_OLDEST)
      .shareIn(scope, SharingStarted.Eagerly, 1)

  private fun initAyahActionPanel() {
    slidingPanel = findViewById(R.id.sliding_panel)
    val slidingLayout =
      slidingPanel.findViewById<ViewGroup>(R.id.sliding_layout)
    slidingPager = slidingPanel
      .findViewById(R.id.sliding_layout_pager)
    val slidingPageIndicator =
      slidingPanel
        .findViewById<IconPageIndicator>(R.id.sliding_pager_indicator)

    // Find close button and set listener
    val closeButton = slidingPanel
      .findViewById<View>(R.id.sliding_menu_close)
    closeButton.setOnClickListener { v: View? -> endAyahMode() }

    // Create and set fragment pager adapter
    slidingPagerAdapter = SlidingPagerAdapter(
      supportFragmentManager,
      QuranUtils.isRtl(),
      additionalAyahPanels,
      includeAudioPanel = BuildConfig.AUDIO_ENABLED
    )
    slidingPager.setAdapter(slidingPagerAdapter)

    // Attach the view pager to the action bar
    slidingPageIndicator.setViewPager(slidingPager)

    // Set sliding layout parameters
    val displayHeight = resources.displayMetrics.heightPixels
    slidingLayout.layoutParams.height = (displayHeight * PANEL_MAX_HEIGHT).toInt()
    slidingPanel.setEnableDragViewTouchEvents(true)
    slidingPanel.setPanelSlideListener(SlidingPanelListener())
    slidingLayout.visibility = View.GONE

    // When clicking any menu items, expand the panel
    slidingPageIndicator.setOnClickListener { v: View? ->
      if (!slidingPanel.isExpanded) {
        slidingPanel.expandPane()
      }
    }
  }

  private fun onPageClicked() {
    // the top bar is permanent, so tapping the page no longer shows or hides it
  }

  private fun onAudioPlaybackAyahChanged(suraAyah: SuraAyah?) {
    if (suraAyah != null) {
      // continue to snap back to the page when the playback ayah changes
      ensurePage(suraAyah.sura, suraAyah.ayah)
    }
  }

  private fun onAyahSelectionChanged(ayahSelection: AyahSelection) {
    val haveSelection = ayahSelection !== AyahSelection.None
    val currentSelection = ayahSelection.selectionIndicator()
    if (currentSelection is SelectionIndicator.None && haveSelection) {
      viewPager.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    if (haveSelection) {
      ayahLabel.value = ayahLabelFor(ayahSelection)
      val startPosition = startPosition(ayahSelection)
      updateLocalTranslations(startPosition)
      onClearAyahModeBackCallback.isEnabled = selectionStart != null
    } else {
      endAyahMode()
      onClearAyahModeBackCallback.isEnabled = false
    }
  }

  /** "2:11", "2:11-13" or "2:286-3:1": what the selection is called in the window's row. */
  private fun ayahLabelFor(selection: AyahSelection): String {
    val start = selection.startSuraAyah() ?: return ""
    val end = selection.endSuraAyah() ?: start
    val first = getString(R.string.sura_ayah, start.sura, start.ayah)
    return when {
      end == start -> first
      end.sura == start.sura -> "$first\u2013${end.ayah}"
      else -> "$first\u2013" + getString(R.string.sura_ayah, end.sura, end.ayah)
    }
  }

  private fun startPosition(ayahSelection: AyahSelection): SuraAyah? {
    return when (ayahSelection) {
      is AyahSelection.Ayah -> {
        ayahSelection.suraAyah
      }

      is AyahRange -> {
        ayahSelection.startSuraAyah
      }

      else -> {
        null
      }
    }
  }

  override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
    val navigate = (audioStatusRepositoryBridge.audioRequest() == null
        && quranSettings.navigateWithVolumeKeys())
    if (navigate && keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
      viewPager.currentItem -= 1
      return true
    } else if (navigate && keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
      viewPager.currentItem += 1
      return true
    }
    return super.onKeyDown(keyCode, event)
  }

  override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
    return (((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
        keyCode == KeyEvent.KEYCODE_VOLUME_UP) && (
        audioStatusRepositoryBridge.audioRequest() == null) &&
        PreferenceManager.getDefaultSharedPreferences(this)
          .getBoolean(Constants.PREF_USE_VOLUME_KEY_NAV, false))
        || super.onKeyUp(keyCode, event))
  }

  public override fun onResume() {
    super.onResume()

    audioPresenter.bind(this)
    recentPagePresenter.bind(currentPageFlow)
    readingBookmarkPresenter.bind(currentPageFlow, this)

    if (shouldReconnect && BuildConfig.AUDIO_ENABLED) {
      foregroundDisposable.add(
        Completable.timer(500, TimeUnit.MILLISECONDS)
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe {
            try {
              startService(
                audioUtils.getAudioIntent(
                  this@PagerActivity,
                  AudioService.ACTION_CONNECT
                )
              )
            } catch (ise: IllegalStateException) {
              // we're likely in the background, so ignore.
            }
            shouldReconnect = false
          })
    }
  }

  override fun inject(audioBarWrapper: AudioBarWrapper) {
    pagerActivityComponent.inject(audioBarWrapper)
  }

  override fun injectQariListWrapper(qariListWrapper: QariListWrapper) {
    pagerActivityComponent.inject(qariListWrapper)
  }

  override fun injectAyahBookmarkWrapper(ayahBookmarkWrapper: AyahBookmarkWrapper) {
    pagerActivityComponent.inject(ayahBookmarkWrapper)
  }

  override fun injectReadingBookmarkSheetWrapper(
    readingBookmarkSheetWrapper: ReadingBookmarkSheetWrapper
  ) {
    pagerActivityComponent.inject(readingBookmarkSheetWrapper)
  }

  override fun provideQuranReadingActivityComponent(): QuranReadingActivityComponent {
    return pagerActivityComponent
  }

  override fun provideQuranReadingPageComponent(vararg pages: Int): QuranReadingPageComponent {
    return pagerActivityComponent
      .quranPageComponentFactory()
      .generate(pages)
  }

  override fun injectToolBar(ayahToolBar: AyahToolBar) {
    pagerActivityComponent
      .inject(ayahToolBar)
  }

  fun showGetRequiredFilesDialog() {
    if (promptDialog != null) {
      return
    }
    val builder = AlertDialog.Builder(this)
    builder.setMessage(R.string.download_extra_data)
      .setPositiveButton(
        R.string.downloadPrompt_ok
      ) { dialog: DialogInterface, option: Int ->
        downloadRequiredFiles()
        dialog.dismiss()
        promptDialog = null
      }
      .setNegativeButton(
        R.string.downloadPrompt_no
      ) { dialog: DialogInterface, option: Int ->
        dialog.dismiss()
        promptDialog = null
      }
    promptDialog = builder.create()
    promptDialog!!.show()
  }

  private fun downloadRequiredFiles() {
    var downloadType = QuranDownloadService.DOWNLOAD_TYPE_AUDIO
    if (audioStatusRepositoryBridge.audioRequest() == null) {
      // if we're not playing any audio, use audio download bar as our progress bar
    } else {
      // if audio is playing, let's not disrupt it - do this using a
      // different type so the broadcast receiver ignores it.
      downloadType = QuranDownloadService.DOWNLOAD_TYPE_ARABIC_SEARCH_DB
    }

    var haveDownload = false
    if (!quranFileUtils.haveAyaPositionFile()) {
      var url = quranFileUtils.ayaPositionFileUrl
      if (isDualPages) {
        url = quranFileUtils.getAyaPositionFileUrl(
          quranScreenInfo.tabletWidthParam
        )
      }
      val destination = quranFileUtils.quranAyahDatabaseDirectory
      // start the download
      val notificationTitle = getString(R.string.highlighting_database)
      val intent = getDownloadIntent(
        this, url,
        destination.absolutePath, notificationTitle, AUDIO_DOWNLOAD_KEY,
        downloadType
      )
      Timber.d("starting service to download ayah position file")
      startService(intent)

      haveDownload = true
    }

    if (!quranFileUtils.hasArabicSearchDatabase()) {
      val url = quranFileUtils.arabicSearchDatabaseUrl

      // show "downloading required files" unless we already showed that for
      // highlighting database, in which case show "downloading search data"
      var notificationTitle = getString(R.string.highlighting_database)
      if (haveDownload) {
        notificationTitle = getString(R.string.search_data)
      }

      val extension = if (url.endsWith(".zip")) ".zip" else ""
      val intent = getDownloadIntent(
        this, url,
        quranFileUtils.getQuranDatabaseDirectory().absolutePath, notificationTitle,
        AUDIO_DOWNLOAD_KEY, downloadType
      )
      intent.putExtra(
        QuranDownloadService.EXTRA_OUTPUT_FILE_NAME,
        QuranDataProvider.QURAN_ARABIC_DATABASE + extension
      )
      Timber.d("starting service to download arabic database")
      startService(intent)
    }

    if (downloadType != QuranDownloadService.DOWNLOAD_TYPE_AUDIO || !BuildConfig.AUDIO_ENABLED) {
      // if audio is playing (or the audio bar, which doubles as the progress bar, is hidden
      // because audio is disabled), just show a status notification
      makeText(
        this, com.quran.mobile.common.download.R.string.downloading,
        Toast.LENGTH_SHORT
      ).show()
    }
  }

  public override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)

    recentPagePresenter.onJump()
    val extras = intent.extras
    if (extras != null) {
      val page = extras.getInt("page", Constants.PAGES_FIRST)

      val currentValue = showingTranslation
      showingTranslation = extras.getBoolean(EXTRA_JUMP_TO_TRANSLATION, showingTranslation)
      val highlightedSura = extras.getInt(EXTRA_HIGHLIGHT_SURA, -1)
      val highlightedAyah = extras.getInt(EXTRA_HIGHLIGHT_AYAH, -1)
      if (highlightedSura > 0 && highlightedAyah > 0) {
        readingEventPresenterBridge.setSelection(highlightedSura, highlightedAyah, true)
      }

      if (showingTranslation != currentValue) {
        if (showingTranslation) {
          pagerAdapter.setTranslationMode()
          updateActionBarSpinner()
          onShowingTranslationBackCallback.isEnabled = true
        } else {
          pagerAdapter.setQuranMode()
          updateActionBarTitle(page)
          onShowingTranslationBackCallback.isEnabled = false
        }

        updateReaderBar()
      }

      if (highlightedAyah > 0 && highlightedSura > 0) {
        // this will jump to the right page automagically
        ensurePage(highlightedSura, highlightedAyah)
      } else {
        val pagePosition = quranInfo.getPositionFromPage(page, isDualPageVisible)
        viewPager.currentItem = pagePosition
      }

      setIntent(intent)
    }
  }

  override fun jumpTo(page: Int) {
    quranNavigator.jumpTo(Page(page))
  }

  override fun jumpToAndHighlight(page: Int, sura: Int, ayah: Int) {
    quranNavigator.jumpTo(SuraAyah(sura, ayah))
  }

  public override fun onPause() {
    foregroundDisposable.clear()
    promptDialog?.dismiss()
    promptDialog = null
    recentPagePresenter.unbind()
    readingBookmarkPresenter.unbind(this)
    quranSettings.wasShowingTranslation = pagerAdapter.isShowingTranslation

    super.onPause()
  }

  override fun onStop() {
    // the activity will be paused when requesting notification
    // permissions, which will otherwise break audio presenter.
    audioPresenter.unbind(this)
    super.onStop()
  }

  override fun onDestroy() {
    Timber.d("onDestroy()")

    translationJob?.cancel(CancellationException())
    compositeDisposable.dispose()
    audioStatusRepositoryBridge.dispose()
    readingEventPresenterBridge.dispose()
    downloadBridge.unsubscribe()
    scope.cancel()
    dismissProgressDialog()
    super.onDestroy()
  }

  private fun onSessionEnd() {
    onEndSessionBackCallback.isEnabled = false
    pagerActivityRecitationPresenter.onSessionEnd()
  }

  public override fun onSaveInstanceState(state: Bundle) {
    val lastPage = quranInfo.getPageFromPosition(viewPager.currentItem, isDualPageVisible)
    state.putInt(LAST_READ_PAGE, lastPage)
    state.putBoolean(LAST_READING_MODE_IS_TRANSLATION, showingTranslation)
    state.putBoolean(LAST_WAS_DUAL_PAGES, isDualPages)
    state.putBoolean(LAST_FOLDING_STATE, isFoldableDeviceOpenAndVertical)
    super.onSaveInstanceState(state)
  }

  private val readerActions by lazy {
    ReaderBarActions(
      onBack = {
        onSessionEnd()
        finish()
      },
      onBookmark = { showReadingBookmarkSheet(ReadingBookmarkTarget.Page(currentPage)) },
      onTranslationChecked = ::onTranslationChecked,
      onMoreTranslations = ::startTranslationManager,
      onReadingMode = { mode ->
        quranSettings.setReadingMode(mode)
        readerBar.readingMode = mode
        applyReadingBackground()
        refreshQuranPages()
      },
      onSearch = { startActivity(Intent(this, SearchActivity::class.java)) },
      onGoToPage = { JumpFragment().show(supportFragmentManager, JumpFragment.TAG) },
      onSettings = { startActivity(Intent(this, QuranPreferenceActivity::class.java)) },
      onHelp = { startActivity(Intent(this, HelpActivity::class.java)) }
    )
  }

  /** The top bar is Compose and always visible; the page sits underneath it, never behind it. */
  private fun setupReaderBar() {
    findViewById<ComposeView>(R.id.reader_toolbar).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent { QuranTheme { ReaderTopBar(readerBar, readerActions) } }
    }

    // the top bar takes care of the status bar, so the page only has to clear the navigation bar
    // (listening on the root rather than the page area, since the compose bar consumes insets
    // that are dispatched to it)
    val pageArea = findViewById<View>(R.id.reader_page_area)
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.sliding_panel)) { _, windowInsets ->
      val insets = windowInsets.getInsets(
        WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.displayCutout()
      )
      pageArea.updatePadding(insets.left, 0, insets.right, 0)
      // the page reaches the bottom edge behind the transparent navigation bar; only the floating
      // choices keep clear of it
      findViewById<View>(R.id.reader_modes).updateLayoutParams<FrameLayout.LayoutParams> {
        bottomMargin = insets.bottom
      }
      windowInsets
    }

    // below the page, where the navigation bar is transparent, the page's own color shows
    window.navigationBarColor = android.graphics.Color.TRANSPARENT
    if (android.os.Build.VERSION.SDK_INT >= 29) {
      window.isNavigationBarContrastEnforced = false
    }
    applyReadingBackground()

    findViewById<ComposeView>(R.id.reader_modes).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent { QuranTheme { ReaderModeBar(readerBar.view, ::onReaderView) } }
    }
    findViewById<ComposeView>(R.id.reader_marker).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent { QuranTheme { MarkerPill(readerBar.marker) } }
    }
    // when the audio bar is up it takes the bottom of the page, so the choices sit above it
    audioStatusBar.addOnLayoutChangeListener { v, _, top, _, bottom, _, _, _, _ ->
      findViewById<View>(R.id.reader_modes).translationY =
        if (v.isVisible) -(bottom - top).toFloat() else 0f
    }

    readerBar.readingMode = quranSettings.readingMode
    readerBar.arabicShown = quranSettings.wantArabicInTranslationView()
    readerBar.showingTranslation = showingTranslation
  }

  /** A new juz or hizb starts on this page: say so just above the reading choices, then let it go. */
  private fun showMarkerPopup(page: Int) {
    if (System.currentTimeMillis() - lastPopupTime < 3000) {
      return
    }
    val text = QuranDisplayHelper.markerPopupText(this, quranInfo, page) ?: return
    lastPopupTime = System.currentTimeMillis()
    readerBar.marker = text
    markerJob?.cancel()
    markerJob = lifecycleScope.launch {
      delay(2500)
      readerBar.marker = null
    }
  }

  /** The reading mode's paper color behind everything, so the strip under the page matches it. */
  private fun applyReadingBackground() {
    val color = when {
      quranSettings.isNightMode -> {
        val level = quranSettings.nightModeBackgroundBrightness
        android.graphics.Color.rgb(level, level, level)
      }
      quranSettings.isSepiaMode -> 0xFFF4E8CC.toInt()
      else -> 0xFFFDFBEF.toInt()
    }
    findViewById<View>(R.id.sliding_panel).setBackgroundColor(color)
    // the bars sit on the paper, so their icons follow the paper rather than the app theme
    val darkPaper = quranSettings.isNightMode
    windowInsetsController.isAppearanceLightStatusBars = !darkPaper
    windowInsetsController.isAppearanceLightNavigationBars = !darkPaper
  }

  /** The bottom choices: the page, or the translation view with or without the Arabic. */
  private fun onReaderView(view: ReaderView) {
    when (view) {
      ReaderView.PAGE -> if (showingTranslation) switchToQuran()
      ReaderView.BOTH, ReaderView.TRANSLATION -> {
        val arabic = view == ReaderView.BOTH
        if (arabic != quranSettings.wantArabicInTranslationView()) {
          quranSettings.setAyahBeforeTranslation(arabic)
          readerBar.arabicShown = arabic
          if (showingTranslation) refreshTranslationPages()
        }
        if (!showingTranslation && translations != null) {
          quranEventLogger.switchToTranslationMode(translations!!.size)
          switchToTranslation()
        }
      }
    }
  }

  /** Called when what the bar shows (translation mode, translation list) may have changed. */
  private fun updateReaderBar() {
    readerBar.showingTranslation = showingTranslation
    readerBar.readingMode = quranSettings.readingMode
  }

  private fun refreshQuranPages() {
    val pos = viewPager.currentItem
    val start = if (pos == 0) 0 else pos - 1
    val end = if (pos == pagerAdapter.count - 1) pos else pos + 1
    for (i in start..end) {
      val f = pagerAdapter.getFragmentIfExists(i)
      if (f is QuranPage) {
        (f as QuranPage).updateView()
      }
    }
  }

  private fun switchToQuran() {
    if (selectionStart != null) {
      endAyahMode()
    }
    val page = currentPage
    pagerAdapter.setQuranMode()
    showingTranslation = false
    onShowingTranslationBackCallback.isEnabled = false
    if (shouldUpdatePageNumber()) {
      val position = quranInfo.getPositionFromPage(page, true)
      viewPager.currentItem = position
    }

    updateReaderBar()
    updateActionBarTitle(page)
  }

  private fun switchToTranslation() {
    if (selectionStart != null) {
      endAyahMode()
    }

    if (translations!!.isEmpty()) {
      startTranslationManager()
    } else {
      val page = currentPage
      pagerAdapter.setTranslationMode()
      showingTranslation = true
      onShowingTranslationBackCallback.isEnabled = true
      if (shouldUpdatePageNumber()) {
        val position = quranInfo.getPositionFromPage(page, false)
        viewPager.currentItem = position
      }
      updateReaderBar()
      updateActionBarSpinner()
    }

    if (!quranFileUtils.hasArabicSearchDatabase() && !promptedForExtraDownload) {
      promptedForExtraDownload = true
      showGetRequiredFilesDialog()
    }
  }

  fun startTranslationManager() {
    startActivity(Intent(this, TranslationManagerActivity::class.java))
  }

  override fun onAddTagSelected() {
    val fm = supportFragmentManager
    val dialog = AddTagDialog()
    dialog.show(fm, AddTagDialog.TAG)
  }

  private fun updateActionBarTitle(page: Int) {
    readerBar.title = quranDisplayData.getSuraNameFromPage(this, page, true)
    readerBar.subtitle = quranDisplayData.getReaderSubtitle(this, page)
  }

  private fun refreshActionBarSpinner() {
    updateActionBarTitle(currentPage)
  }

  private val currentPage: Int
    get() = quranInfo.getPageFromPosition(viewPager.currentItem, isDualPageVisible)

  /** The translations list in the top bar; the title itself is the same as in Quran mode. */
  private fun updateActionBarSpinner() {
    rebuildTranslationItems()
    updateActionBarTitle(currentPage)
  }

  private fun rebuildTranslationItems() {
    val active = activeTranslationsFilesNames ?: quranSettings.activeTranslations
    val list = translations.orEmpty()
    readerBar.translations = list.mapIndexed { index, translation ->
      ReaderTranslationItem(
        filename = translation.filename,
        name = translationNames.getOrNull(index) ?: translation.filename,
        checked = active.contains(translation.filename)
      )
    }
  }

  private fun onTranslationChecked(filename: String) {
    val selected = HashSet(activeTranslationsFilesNames ?: quranSettings.activeTranslations)
    if (!selected.remove(filename)) {
      selected.add(filename)
    }
    // there is always at least one translation to read
    if (selected.isEmpty()) {
      return
    }
    activeTranslationsFilesNames = selected
    quranSettings.activeTranslations = selected
    rebuildTranslationItems()
    refreshTranslationPages()
  }

  /** An action of the selected ayah, from the row beside its number on a translation page. */
  fun performAyahAction(itemId: Int) {
    ayahToolBar.performItem(itemId)
  }

  private fun updateSelectedVerseBookmark(bookmarked: Boolean) {
    val pos = viewPager.currentItem - 1
    for (count in 0..2) {
      if (pos + count < 0) continue
      (pagerAdapter.getFragmentIfExists(pos + count) as? TranslationFragment)
        ?.setSelectedBookmarked(bookmarked)
    }
  }

  private fun refreshTranslationPages() {
    // the translation in the ayah window follows the switches too
    (supportFragmentManager.findFragmentById(ayahToolBar.contentContainer.id) as? AyahTranslationFragment)
      ?.refreshView()
    val pos = viewPager.currentItem - 1
    for (count in 0..2) {
      if (pos + count < 0) {
        continue
      }
      val f = pagerAdapter.getFragmentIfExists(pos + count)
      if (f is TranslationFragment) {
        f.refresh()
      } else if (f is TabletFragment) {
        f.refresh()
      }
    }
  }

  private fun onDownloadSuccess() {
    refreshQuranPages()
    audioPresenter.onDownloadSuccess()
  }

  /** The top bar is permanent now; these stay only so existing callers keep compiling. */
  @Suppress("UNUSED_PARAMETER")
  fun toggleActionBarVisibility(visible: Boolean) {
  }

  fun toggleActionBar() {
  }

  private fun ensurePage(sura: Int, ayah: Int) {
    val page = quranInfo.getPageFromSuraAyah(sura, ayah)
    if (quranInfo.isValidPage(page)) {
      val position = quranInfo.getPositionFromPage(page, isDualPageVisible)
      if (position != viewPager.currentItem) {
        viewPager.currentItem = position
      }
    }
  }

  private fun requestTranslationsList() {
    translationJob =
      translationListPresenter.registerForTranslations { titles: Array<String>, updatedTranslations: List<LocalTranslation> ->
        var currentActiveTranslationsFilesNames = quranSettings.activeTranslations
        if (currentActiveTranslationsFilesNames.isEmpty() && updatedTranslations.isNotEmpty()) {
          currentActiveTranslationsFilesNames = HashSet()
          val items = updatedTranslations.size
          for (i in 0 until items) {
            currentActiveTranslationsFilesNames.add(updatedTranslations[i].filename)
          }
        }
        activeTranslationsFilesNames = currentActiveTranslationsFilesNames

        translationNames = titles
        translations = updatedTranslations
        rebuildTranslationItems()
      }
  }

  private fun refreshBookmarksMenu(isBookmarked: Boolean) {
    readerBar.isBookmarked = isBookmarked
  }

  override fun setPageReadingBookmarkSelected(isBookmarked: Boolean) {
    isCurrentPageReadingBookmarked = isBookmarked
    refreshBookmarksMenu(isBookmarked)
  }

  override fun showReadingBookmarkChangedToast(
    change: ReadingBookmarkChange,
    isEducation: Boolean,
    onUndo: () -> Unit,
    onDismiss: () -> Unit
  ) {
    val rootView = findViewById<ViewGroup>(android.R.id.content)
    readingBookmarkToastView?.let { rootView.removeView(it) }

    val toastView = createReadingBookmarkToastView(
      this,
      change,
      isEducation,
      quranNaming,
      onUndo = onUndo,
      onDismiss = onDismiss
    )
    readingBookmarkToastView = toastView
    rootView.addView(
      toastView,
      FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.WRAP_CONTENT,
        Gravity.BOTTOM
      )
    )
    // the sliding panel that hosts the rest of the reading UI claims every touch within its
    // collapsed (audio bar) region for its own drag/tap handling, regardless of what's drawn on
    // top of it there - so rather than fight that, sit the toast just above the audio bar (when
    // one is visible, its own height already accounts for the navigation bar inset) or above the
    // navigation bar directly (when it isn't, e.g. three-button navigation in edge-to-edge mode).
    // side padding covers landscape three-button navigation and display cutouts.
    ViewCompat.setOnApplyWindowInsetsListener(toastView) { view, windowInsets ->
      val insets = windowInsets.getInsets(
        WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.displayCutout()
      )
      view.updatePadding(left = insets.left, right = insets.right)
      val params = view.layoutParams as FrameLayout.LayoutParams
      // above the floating reading choices (48dp, plus room for their shadow)
      val readingChoices = (64 * resources.displayMetrics.density).toInt()
      params.bottomMargin = readingChoices + if (audioStatusBar.isVisible) {
        audioStatusBar.height
      } else {
        insets.bottom
      }
      view.layoutParams = params
      windowInsets
    }
    ViewCompat.requestApplyInsets(toastView)
  }

  override fun dismissReadingBookmarkChangedToast() {
    readingBookmarkToastView?.let {
      findViewById<ViewGroup>(android.R.id.content).removeView(it)
    }
    readingBookmarkToastView = null
  }

  // region Audio playback
  private fun currentQariItem(): QariItem {
    return fromQari(this, qariManager.currentQari())
  }

  override fun onContinuePlaybackPressed() {
    handlePlayback(null)
  }

  override fun onPlayPressed() {
    val position = viewPager.currentItem
    val page = quranInfo.getPageFromPosition(position, isDualPageVisible)

    // log the event
    quranEventLogger.logAudioPlayback(
      QuranEventLogger.AudioPlaybackSource.PAGE,
      currentQariItem(), isDualPages, showingTranslation, isSplitScreen
    )

    val startSura = quranDisplayData.safelyGetSuraOnPage(page)
    val startAyah = quranInfo.getFirstAyahOnPage(page)
    val startingSuraList = quranInfo.getListOfSurahWithStartingOnPage(page)
    if (startingSuraList.isEmpty() ||
      (startingSuraList.size == 1 && startingSuraList[0] == startSura)
    ) {
      playFromAyah(startSura, startAyah)
    } else {
      promptForMultipleChoicePlay(page, startSura, startAyah, startingSuraList)
    }
  }

  private fun playFromAyah(startSura: Int, startAyah: Int) {
    val page = quranInfo.getPageFromSuraAyah(startSura, startAyah)
    val start = SuraAyah(startSura, startAyah)
    val end = selectionEnd
    // handle the case of multiple ayat being selected and play them as a range if so
    val ending = if ((end == null || start == end || start.after(end))) null else end
    playFromAyah(start, ending, page, 0, 0, ending != null, 1.0f)
  }

  fun playFromAyah(
    start: SuraAyah,
    end: SuraAyah?,
    page: Int,
    verseRepeat: Int,
    rangeRepeat: Int,
    enforceRange: Boolean,
    playbackSpeed: Float
  ) {
    val ending = end
      ?: audioUtils.getLastAyahToPlay(
        start, page,
        quranSettings.preferredDownloadAmount, isDualPageVisible
      )

    if (ending != null) {
      Timber.d(
        "playFromAyah - " + start + ", ending: " +
            ending + " - original: " + end + " -- " +
            quranSettings.preferredDownloadAmount
      )
      val item = currentQariItem()
      val shouldStream = quranSettings.shouldStream()
      audioPresenter.play(
        start,
        ending,
        item,
        verseRepeat,
        rangeRepeat,
        enforceRange,
        playbackSpeed,
        shouldStream
      )
    }
  }

  override fun handleRequiredDownload(downloadIntent: Intent?) {
    var needsPermission = needsPermissionToDownloadOver3g
    if (needsPermission) {
      if (QuranUtils.isOnWifiNetwork(this)) {
        Timber.d("on wifi, don't need permission for download...")
        needsPermission = false
      }
    }

    if (needsPermission) {
      downloadInfoStreams.requestDownloadNetworkPermission()
    } else if (!havePostNotificationPermission(this)) {
      if (canRequestPostNotificationPermission(this)) {
        promptDialog = buildPostPermissionDialog(
          this,
          {
            promptDialog = null
            requestPermissionLauncher!!.launch(Manifest.permission.POST_NOTIFICATIONS)
          }, {
            proceedWithDownload(downloadIntent)
            promptDialog = null
          })
        promptDialog!!.show()
      } else {
        requestPermissionLauncher!!.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    } else {
      proceedWithDownload(downloadIntent)
    }
  }

  override fun proceedWithDownload(downloadIntent: Intent?) {
    downloadInfoStreams.downloadRequested()
    Timber.d("starting service in handleRequiredDownload")
    startService(downloadIntent)
  }

  override fun handlePlayback(request: AudioRequest?) {
    needsPermissionToDownloadOver3g = true
    val intent = Intent(this, AudioService::class.java)
    intent.setAction(AudioService.ACTION_PLAYBACK)
    if (request != null) {
      intent.putExtra(AudioService.EXTRA_PLAY_INFO, request)
    }

    Timber.d("starting service for audio playback")
    startService(intent)
  }

  override fun onPausePressed() {
    startService(
      audioUtils.getAudioIntent(
        this, AudioService.ACTION_PAUSE
      )
    )
  }

  override fun setPlaybackSpeed(speed: Float) {
    val lastAudioRequest = audioStatusRepositoryBridge.audioRequest()
    if (lastAudioRequest != null) {
      val updatedAudioRequest = lastAudioRequest.copy(playbackSpeed = speed)

      val i = Intent(this, AudioService::class.java)
      i.setAction(AudioService.ACTION_UPDATE_SETTINGS)
      i.putExtra(AudioService.EXTRA_PLAY_INFO, updatedAudioRequest)
      startService(i)
    }
  }

  override fun onNextPressed() {
    startService(
      audioUtils.getAudioIntent(
        this,
        AudioService.ACTION_SKIP
      )
    )
  }

  override fun onPreviousPressed() {
    startService(
      audioUtils.getAudioIntent(
        this,
        AudioService.ACTION_REWIND
      )
    )
  }

  override fun onAudioSettingsPressed() {
    showSlider(slidingPagerAdapter.getPagePosition(SlidingPagerAdapter.AUDIO_PAGE))
  }

  override fun onShowQariList() {
    val page = currentPage
    val start = selectionStart
    val end = selectionEnd

    val startSura = quranDisplayData.safelyGetSuraOnPage(page)
    val startAyah = quranInfo.getFirstAyahOnPage(page)

    val starting = start ?: SuraAyah(startSura, startAyah)
    val ending = end
      ?: audioUtils.getLastAyahToPlay(
        starting, page,
        quranSettings.preferredDownloadAmount, isDualPageVisible
      )!!
    val qariListWrapper = QariListWrapper(this, starting, ending)
    overlay.removeAllViews()
    overlay.addView(
      qariListWrapper,
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT
    )
    overlay.visibility = View.VISIBLE
    toggleActionBarVisibility(false)
  }

  private fun showReadingBookmarkSheet(target: ReadingBookmarkTarget) {
    val readingBookmarkSheetWrapper = ReadingBookmarkSheetWrapper(
      context = this,
      target = target,
      onDismissed = {
        overlay.removeAllViews()
        overlay.visibility = View.GONE
        toggleActionBarVisibility(true)
      }
    )
    overlay.removeAllViews()
    overlay.addView(
      readingBookmarkSheetWrapper,
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT
    )
    overlay.visibility = View.VISIBLE
    toggleActionBarVisibility(false)
  }

  private fun showAyahBookmarkSheet(suraAyah: SuraAyah) {
    val ayahBookmarkWrapper = AyahBookmarkWrapper(this, suraAyah) { isBookmarked ->
      overlay.removeAllViews()
      overlay.visibility = View.GONE
      toggleActionBarVisibility(true)
      updateAyahBookmark(suraAyah, isBookmarked)
    }
    overlay.removeAllViews()
    overlay.addView(
      ayahBookmarkWrapper,
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT
    )
    overlay.visibility = View.VISIBLE
    toggleActionBarVisibility(false)
  }

  fun updatePlayOptions(
    rangeRepeat: Int,
    verseRepeat: Int,
    enforceRange: Boolean,
    playbackSpeed: Float
  ): Boolean {
    val lastAudioRequest = audioStatusRepositoryBridge.audioRequest()
    if (lastAudioRequest != null) {
      val updatedAudioRequest = lastAudioRequest.copy(
        repeatInfo = verseRepeat,
        rangeRepeatInfo = rangeRepeat,
        enforceBounds = enforceRange,
        playbackSpeed = playbackSpeed
      )
      val i = Intent(this, AudioService::class.java)
      i.setAction(AudioService.ACTION_UPDATE_SETTINGS)
      i.putExtra(AudioService.EXTRA_PLAY_INFO, updatedAudioRequest)
      startService(i)
      return true
    } else {
      return false
    }
  }

  override fun setRepeatCount(repeatCount: Int) {
    val lastAudioRequest = audioStatusRepositoryBridge.audioRequest()
    if (lastAudioRequest != null) {
      val updatedAudioRequest = lastAudioRequest.copy(repeatInfo = repeatCount)

      val i = Intent(this, AudioService::class.java)
      i.setAction(AudioService.ACTION_UPDATE_SETTINGS)
      i.putExtra(AudioService.EXTRA_PLAY_INFO, updatedAudioRequest)
      startService(i)
    }
  }

  override fun onStopPressed() {
    startService(audioUtils.getAudioIntent(this, AudioService.ACTION_STOP))
  }

  override fun onCancelPressed(cancelDownload: Boolean) {
    if (cancelDownload) {
      needsPermissionToDownloadOver3g = true

      val i = Intent(this, QuranDownloadService::class.java)
      i.setAction(QuranDownloadService.ACTION_CANCEL_DOWNLOADS)
      startService(i)
    } else {
      startService(audioUtils.getAudioIntent(this, AudioService.ACTION_STOP))
    }
  }

  override fun onAcceptPressed() {
    needsPermissionToDownloadOver3g = false
    audioPresenter.onDownloadPermissionGranted()
  }

  //endregion

  private val selectionStart: SuraAyah?
    // region Ayah selection
    get() {
      val currentSelection = readingEventPresenter.currentAyahSelection()
      return currentSelection.startSuraAyah()
    }

  private val selectionEnd: SuraAyah?
    get() {
      val currentSelection = readingEventPresenter.currentAyahSelection()
      return currentSelection.endSuraAyah()
    }

  val lastAudioRequest: AudioRequest?
    get() = audioStatusRepositoryBridge.audioRequest()

  fun endAyahMode() {
    readingEventPresenterBridge.clearSelectedAyah()
    slidingPanel.collapsePane()
  }

  //endregion
  private fun updateLocalTranslations(start: SuraAyah?) {
    val ayahTracker = resolveCurrentTracker()
    if (ayahTracker != null) {
      lastActivatedLocalTranslations = ayahTracker.getLocalTranslations() ?: emptyArray()
      lastSelectedTranslationAyah = ayahTracker.getQuranAyahInfo(start!!.sura, start.ayah)
    }
  }

  private fun resolveCurrentTracker(): AyahTracker? {
    val position = viewPager.currentItem
    val f = pagerAdapter.getFragmentIfExists(position)
    return if (f is QuranPage && f.isVisible) {
      (f as QuranPage).getAyahTracker()
    } else {
      null
    }
  }

  private inner class AyahMenuItemSelectionHandler : MenuItem.OnMenuItemClickListener {
    override fun onMenuItemClick(item: MenuItem): Boolean {
      var sliderPage = -1
      val currentSelection = readingEventPresenter.currentAyahSelection()
      val startSuraAyah = currentSelection.startSuraAyah()
      val endSuraAyah = currentSelection.endSuraAyah()
      if (startSuraAyah == null || endSuraAyah == null) {
        return false
      }

      val itemId = item.itemId
      if (itemId == com.quran.labs.androidquran.common.toolbar.R.id.cab_bookmark_ayah) {
        showAyahBookmarkSheet(startSuraAyah)
      } else if (itemId == com.quran.labs.androidquran.common.toolbar.R.id.cab_play_from_here) {
        quranEventLogger.logAudioPlayback(
          QuranEventLogger.AudioPlaybackSource.AYAH,
          currentQariItem(), isDualPages, showingTranslation, isSplitScreen
        )
        playFromAyah(startSuraAyah.sura, startSuraAyah.ayah)
        toggleActionBarVisibility(true)
      } else if (itemId == com.quran.labs.androidquran.common.toolbar.R.id.cab_recite_from_here) {
        onEndSessionBackCallback.isEnabled = true
        pagerActivityRecitationPresenter.onRecitationPressed()
      } else if (itemId == com.quran.labs.androidquran.common.toolbar.R.id.cab_share_ayah_link) {
        shareAyahLink(startSuraAyah, endSuraAyah)
      } else if (itemId == com.quran.labs.androidquran.common.toolbar.R.id.cab_share_ayah_text) {
        shareAyah(startSuraAyah, endSuraAyah, false)
      } else if (itemId == com.quran.labs.androidquran.common.toolbar.R.id.cab_copy_ayah) {
        shareAyah(startSuraAyah, endSuraAyah, true)
      } else if (itemId == com.quran.labs.androidquran.common.toolbar.R.id.cab_close_ayah) {
        // nothing to do: the selection ends just below
      } else {
        return false
      }

      if (sliderPage < 0) {
        endAyahMode()
      } else {
        showSlider(sliderPage)
      }
      return true
    }
  }

  private fun shareAyah(start: SuraAyah?, end: SuraAyah?, isCopy: Boolean) {
    if (start == null || end == null) {
      return
    } else if (!quranFileUtils.hasArabicSearchDatabase()) {
      showGetRequiredFilesDialog()
      return
    }

    val translationNames = lastActivatedLocalTranslations
    if (showingTranslation && translationNames.isNotEmpty()) {
      // temporarily required so "lastSelectedTranslationAyah" isn't null
      // the real solution is to move this sharing logic out of PagerActivity
      // in the future and avoid this back and forth with the translation fragment.

      updateLocalTranslations(start)
      val quranAyahInfo = lastSelectedTranslationAyah
      if (quranAyahInfo != null) {
        val shareText = shareUtil.getShareText(this, quranAyahInfo, translationNames)
        if (isCopy) {
          shareUtil.copyToClipboard(this, shareText)
        } else {
          shareUtil.shareViaIntent(
            this,
            shareText,
            com.quran.labs.androidquran.common.toolbar.R.string.share_ayah_text
          )
        }
      }

      return
    }

    compositeDisposable.add(
      arabicDatabaseUtils
        .getVerses(start, end)
        .filter { quranAyahs: List<QuranText?> -> quranAyahs.isNotEmpty() }
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { quranAyahs: List<QuranText> ->
          if (isCopy) {
            shareUtil.copyVerses(this@PagerActivity, quranAyahs)
          } else {
            shareUtil.shareVerses(this@PagerActivity, quranAyahs)
          }
        })
  }

  fun shareAyahLink(start: SuraAyah, end: SuraAyah) {
    showProgressDialog()
    compositeDisposable.add(
      quranAppUtils.getQuranAppUrlObservable(getString(R.string.quranapp_key), start, end)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeWith(object : DisposableSingleObserver<String>() {
          override fun onSuccess(url: String) {
            shareUtil.shareViaIntent(
              this@PagerActivity,
              url,
              com.quran.labs.androidquran.common.toolbar.R.string.share_ayah
            )
            dismissProgressDialog()
          }

          override fun onError(e: Throwable) {
            dismissProgressDialog()
          }
        })
    )
  }

  private fun showProgressDialog() {
    if (progressDialog == null) {
      progressDialog = ProgressDialog(this).apply {
        isIndeterminate = true
        setMessage(getString(com.quran.mobile.common.ui.core.R.string.loading))
        show()
      }
    }
  }

  private fun dismissProgressDialog() {
    if (progressDialog?.isShowing == true) {
      progressDialog?.dismiss()
    }
    progressDialog = null
  }

  private fun showSlider(sliderPage: Int) {
    readingEventPresenterBridge.clearMenuForSelection()
    slidingPager.currentItem = sliderPage
    slidingPanel.showPane()
    // TODO there's got to be a better way than this hack
    // The issue is that smoothScrollTo returns if mCanSlide is false
    // and it's false when the panel is GONE and showPane only calls
    // requestLayout, and only in onLayout does mCanSlide become true.
    // So by posting this later it gives time for onLayout to run.
    slidingPanel.post { slidingPanel.expandPane() }
  }

  private fun updateAyahBookmark(suraAyah: SuraAyah, bookmarked: Boolean) {
    // Refresh toolbar icon
    val start = selectionStart
    if (start != null && start == suraAyah) {
      ayahToolBar.setBookmarked(bookmarked)
      updateSelectedVerseBookmark(bookmarked)
    }
  }

  private fun promptForMultipleChoicePlay(
    page: Int, startSura: Int, startAyah: Int, startingSuraList: List<Int>
  ) {
    val adapter = ArrayAdapter<String>(this, android.R.layout.select_dialog_item)
    startingSuraList
      .map { quranDisplayData.getSuraName(this, it, false) }
      .onEach { adapter.add(it) }

    val suraList = if (startSura != startingSuraList[0]) {
      adapter.insert(getString(R.string.starting_page_label), 0)
      listOf(startSura) + startingSuraList
    } else {
      startingSuraList
    }

    val builder = AlertDialog.Builder(this)
      .setTitle(getString(R.string.playback_prompt_title))
      .setAdapter(adapter) { dialog: DialogInterface, i: Int ->
        if (i == 0) {
          playFromAyah(startSura, startAyah)
        } else {
          playFromAyah(suraList[i], 1)
        }
        dialog.dismiss()
        promptDialog = null
      }
    promptDialog = builder.create()
    promptDialog!!.show()
  }

  private inner class SlidingPanelListener : SlidingUpPanelLayout.PanelSlideListener {
    override fun onPanelSlide(panel: View, slideOffset: Float) {
    }

    override fun onPanelCollapsed(panel: View) {
      if (selectionStart != null) {
        endAyahMode()
      }
      slidingPanel.hidePane()
      readingEventPresenter.onPanelClosed()
    }

    override fun onPanelExpanded(panel: View) {
      readingEventPresenter.onPanelOpened()
    }

    override fun onPanelAnchored(panel: View) {
    }
  }

  companion object {
    private const val AUDIO_DOWNLOAD_KEY = "AUDIO_DOWNLOAD_KEY"
    private const val LAST_READ_PAGE = "LAST_READ_PAGE"
    private const val LAST_READING_MODE_IS_TRANSLATION = "LAST_READING_MODE_IS_TRANSLATION"
    private const val LAST_ACTIONBAR_STATE = "LAST_ACTIONBAR_STATE"
    private const val LAST_FOLDING_STATE = "LAST_FOLDING_STATE"

    const val EXTRA_JUMP_TO_TRANSLATION: String = "jumpToTranslation"
    const val EXTRA_HIGHLIGHT_SURA: String = "highlightSura"
    const val EXTRA_HIGHLIGHT_AYAH: String = "highlightAyah"
    const val LAST_WAS_DUAL_PAGES: String = "wasDualPages"

    private const val DEFAULT_HIDE_AFTER_TIME: Long = 2000

    const val MSG_HIDE_ACTIONBAR: Int = 1

    // AYAH ACTION PANEL STUFF
    // Max height of sliding panel (% of screen)
    private const val PANEL_MAX_HEIGHT = 0.4f
  }
}
