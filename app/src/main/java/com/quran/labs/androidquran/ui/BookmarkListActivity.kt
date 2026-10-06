package com.quran.labs.androidquran.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.quran.data.dao.BookmarkSortOrder
import com.quran.data.model.Page
import com.quran.data.model.SuraAyah
import com.quran.data.model.highlight.HighlightColor
import com.quran.labs.androidquran.QuranApplication
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.common.ui.core.HighlightColors
import com.quran.labs.androidquran.common.ui.core.QuranTheme
import com.quran.labs.androidquran.dao.bookmark.BookmarkListMode
import com.quran.labs.androidquran.dao.bookmark.BookmarkListRowData
import com.quran.labs.androidquran.presenter.bookmark.BookmarkListPresenter
import com.quran.labs.androidquran.presenter.bookmark.BookmarkPresenter
import com.quran.labs.androidquran.ui.compose.BookmarkListActions
import com.quran.labs.androidquran.ui.compose.BookmarkListScreen
import com.quran.labs.androidquran.ui.fragment.AddTagDialog
import com.quran.labs.androidquran.ui.fragment.TagBookmarkDialog
import com.quran.labs.androidquran.ui.fragment.TagBookmarkDialog.OnBookmarkTagsUpdateListener
import com.quran.labs.androidquran.ui.helpers.QuranNavigator
import com.quran.labs.androidquran.ui.helpers.QuranRow
import com.quran.labs.androidquran.ui.helpers.QuranRowFactory
import com.quran.labs.androidquran.util.QuranSettings
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class BookmarkListActivity : AppCompatActivity(), OnBookmarkTagsUpdateListener {

  private lateinit var mode: BookmarkListMode
  private var rows by mutableStateOf<List<QuranRow>>(emptyList())
  private var selected by mutableStateOf<Set<Int>>(emptySet())
  private var sortOrderState by mutableStateOf(BookmarkSortOrder.SORT_DATE_ADDED)
  private var collectionName by mutableStateOf<String?>(null)
  private var rowData: List<BookmarkListRowData> = emptyList()
  private var isPaused = false
  private val sortOrder = MutableStateFlow(BookmarkSortOrder.SORT_DATE_ADDED)
  private val snackbarHostState = SnackbarHostState()

  @Inject lateinit var quranNavigatorFactory: QuranNavigator.Factory
  private val quranNavigator by lazy { quranNavigatorFactory.create(this) }

  @Inject
  lateinit var bookmarkListPresenter: BookmarkListPresenter

  @Inject
  lateinit var quranRowFactory: QuranRowFactory

  @Inject
  lateinit var quranSettings: QuranSettings

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    (application as QuranApplication).applicationComponent.inject(this)

    val mode = modeFromIntent(intent)
    if (mode == null) {
      finish()
      return
    }
    this.mode = mode

    sortOrder.value = quranSettings.bookmarksSortOrder
    sortOrderState = sortOrder.value

    val actions = BookmarkListActions(
      onBack = ::finish,
      onRowClick = ::onRowClick,
      onRowLongClick = ::toggleSelection,
      onClearSelection = { selected = emptySet() },
      onSortOrder = ::setSortOrder,
      onRename = ::renameCollection,
      onTag = ::tagSelectedBookmarks,
      onDelete = ::removeSelectedRows
    )
    val emptyText = getString(
      when (mode) {
        is BookmarkListMode.Collection -> R.string.collection_empty
        is BookmarkListMode.Highlights -> R.string.highlights_empty
      }
    )
    setContent {
      QuranTheme {
        BookmarkListScreen(
          title = currentTitle(),
          rows = rows,
          selectedIndices = selected,
          sortOrder = sortOrderState,
          canRename = mode is BookmarkListMode.Collection,
          canTag = mode is BookmarkListMode.Collection,
          emptyText = emptyText,
          snackbarHostState = snackbarHostState,
          actions = actions
        )
      }
    }

    // back leaves selection mode before it leaves the screen
    onBackPressedDispatcher.addCallback(
      this,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          if (selected.isNotEmpty()) {
            selected = emptySet()
          } else {
            finish()
          }
        }
      }
    )

    subscribeToData()
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  private fun subscribeToData() {
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        sortOrder
          .flatMapLatest { order -> bookmarkListPresenter.rows(mode, order) }
          .collect { rows -> onNewRowData(rows) }
      }
    }

    val mode = mode
    if (mode is BookmarkListMode.Collection) {
      lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
          bookmarkListPresenter.collectionName(mode.collectionId).collect { name ->
            // the collection can be renamed from this screen, or deleted from elsewhere
            if (name == null) {
              finish()
            } else {
              collectionName = name
            }
          }
        }
      }
    }
  }

  private fun currentTitle(): String = when (val mode = mode) {
    is BookmarkListMode.Collection ->
      collectionName ?: intent.getStringExtra(EXTRA_COLLECTION_NAME).orEmpty()

    is BookmarkListMode.Highlights -> getString(HighlightColors[mode.color].nameResourceId)
  }

  private fun onNewRowData(newRows: List<BookmarkListRowData>) {
    rowData = newRows
    selected = emptySet()
    rows = newRows.map { row ->
      when (row) {
        is BookmarkListRowData.SuraHeader -> quranRowFactory.fromSuraHeader(this, row.sura)
        is BookmarkListRowData.BookmarkItem ->
          quranRowFactory.fromBookmark(this, row.bookmark, row.collectionId)

        is BookmarkListRowData.HighlightItem ->
          quranRowFactory.fromHighlight(this, row.highlight, row.ayahText)
      }
    }
  }

  override fun onStop() {
    isPaused = true
    // the undo window does not follow the user off the screen
    bookmarkListPresenter.flushPendingRemovals()
    super.onStop()
  }

  override fun onStart() {
    super.onStart()
    isPaused = false
  }

  private fun renameCollection() {
    val mode = mode
    if (mode is BookmarkListMode.Collection && !isPaused) {
      AddTagDialog.newInstance(mode.collectionId, collectionName.orEmpty())
        .show(supportFragmentManager, AddTagDialog.TAG)
    }
  }

  private fun setSortOrder(order: Int) {
    if (sortOrder.value == order) {
      return
    }
    // shared with the bookmarks tab, so the two screens agree on how the list is sorted
    quranSettings.bookmarksSortOrder = order
    sortOrder.value = order
    sortOrderState = order
  }

  private fun onRowClick(index: Int, row: QuranRow) {
    if (selected.isNotEmpty()) {
      toggleSelection(index, row)
    } else if (!row.isHeader) {
      jumpToAndHighlight(row.page, row.sura, row.ayah)
    }
  }

  private fun toggleSelection(index: Int, row: QuranRow) {
    if (!isValidSelection(row)) return
    selected = if (index in selected) selected - index else selected + index
  }

  private fun isValidSelection(row: QuranRow): Boolean =
    row.isBookmark || row.isHighlightedAyah

  private fun jumpToAndHighlight(page: Int, sura: Int, ayah: Int) {
    val location = if (sura > 0 && ayah > 0) SuraAyah(sura, ayah) else Page(page)
    quranNavigator.jumpTo(location, showTranslation = quranSettings.wasShowingTranslation)
  }

  private fun removeSelectedRows() {
    val toRemove = selected.mapNotNull { index -> rows.getOrNull(index)?.let(::rowDataFor) }
    selected = emptySet()
    if (toRemove.isEmpty()) return

    bookmarkListPresenter.removeAfterSomeTime(
      toRemove,
      BookmarkPresenter.DELAY_DELETION_DURATION_IN_MS.toLong()
    )

    val size = toRemove.size
    val message = when (mode) {
      is BookmarkListMode.Collection ->
        resources.getQuantityString(R.plurals.bookmark_tag_deleted, size, size)

      is BookmarkListMode.Highlights ->
        resources.getQuantityString(R.plurals.removed_highlights, size, size)
    }
    lifecycleScope.launch {
      // dismissed by this timer, since the delay before the removal is really applied is the
      // presenter's rather than one of the standard snackbar durations
      val dismisser = launch {
        delay(BookmarkPresenter.DELAY_DELETION_DURATION_IN_MS.toLong())
        snackbarHostState.currentSnackbarData?.dismiss()
      }
      val result = snackbarHostState.showSnackbar(
        message = message,
        actionLabel = getString(R.string.undo),
        duration = SnackbarDuration.Indefinite
      )
      dismisser.cancel()
      if (result == SnackbarResult.ActionPerformed) bookmarkListPresenter.cancelRemoval()
    }
  }

  private fun rowDataFor(row: QuranRow): BookmarkListRowData? {
    return rowData.firstOrNull { data ->
      when (data) {
        is BookmarkListRowData.BookmarkItem -> data.bookmark.id == row.bookmarkId
        is BookmarkListRowData.HighlightItem ->
          data.highlight.suraAyah.sura == row.sura && data.highlight.suraAyah.ayah == row.ayah

        is BookmarkListRowData.SuraHeader -> false
      }
    }
  }

  private fun tagSelectedBookmarks() {
    val ids = selected.mapNotNull { rows.getOrNull(it)?.bookmarkId }.toTypedArray()
    if (ids.isEmpty() || isPaused) {
      return
    }
    TagBookmarkDialog.newInstance(ids).show(supportFragmentManager, TagBookmarkDialog.TAG)
  }

  override fun onAddTagSelected() {
    if (!isPaused) {
      AddTagDialog().show(supportFragmentManager, AddTagDialog.TAG)
    }
  }

  companion object {
    private const val EXTRA_COLLECTION_ID = "collectionId"
    private const val EXTRA_COLLECTION_NAME = "collectionName"
    private const val EXTRA_HIGHLIGHT_COLOR = "highlightColor"

    fun collectionIntent(context: Context, collectionId: String, name: String?): Intent {
      return Intent(context, BookmarkListActivity::class.java)
        .putExtra(EXTRA_COLLECTION_ID, collectionId)
        .putExtra(EXTRA_COLLECTION_NAME, name)
    }

    fun highlightsIntent(context: Context, color: HighlightColor): Intent {
      return Intent(context, BookmarkListActivity::class.java)
        .putExtra(EXTRA_HIGHLIGHT_COLOR, color.name)
    }

    private fun modeFromIntent(intent: Intent): BookmarkListMode? {
      val collectionId = intent.getStringExtra(EXTRA_COLLECTION_ID)
      if (collectionId != null) {
        return BookmarkListMode.Collection(collectionId)
      }

      val color = intent.getStringExtra(EXTRA_HIGHLIGHT_COLOR)
        ?.let { name -> HighlightColor.entries.firstOrNull { color -> color.name == name } }
      return color?.let { BookmarkListMode.Highlights(it) }
    }
  }
}
