package com.quran.labs.androidquran.ui.compose

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import com.quran.data.model.bookmark.Tag
import com.quran.data.model.highlight.HighlightColor
import com.quran.labs.androidquran.dao.bookmark.BookmarkRawResult
import com.quran.labs.androidquran.presenter.bookmark.BookmarkPresenter
import com.quran.labs.androidquran.presenter.bookmark.BookmarksView
import com.quran.labs.androidquran.ui.helpers.BookmarkUIConverter
import com.quran.labs.androidquran.ui.helpers.QuranRow

/** What a tap on the bookmarks tab can ask of the rest of the app. The activity owns these. */
class BookmarksActions(
  val onJumpTo: (QuranRow) -> Unit,
  val onOpenCollection: (QuranRow) -> Unit,
  val onOpenHighlightColor: (HighlightColor) -> Unit,
  val onAddTag: () -> Unit,
  val onEditTag: (id: String, name: String?) -> Unit,
  val onTagBookmarks: (ids: Array<String>) -> Unit
)

/**
 * The bookmarks tab's state: the rows [BookmarkPresenter] last produced, which of them are
 * selected, and the sort / grouping options. It replaces the old `BookmarksFragment`, so the
 * home screen can swap its app bar for a selection bar while rows are selected.
 */
class BookmarksState(
  private val context: Context,
  private val presenter: BookmarkPresenter,
  private val converter: BookmarkUIConverter
) : BookmarksView {
  var rows by mutableStateOf<List<QuranRow>>(emptyList())
    private set
  var tagMap by mutableStateOf<Map<String, Tag>>(emptyMap())
    private set
  var showTags by mutableStateOf(false)
    private set

  /** false until the first load finishes, so the empty state doesn't flash while loading. */
  var isLoaded by mutableStateOf(false)
    private set

  var sortOrder by mutableIntStateOf(presenter.getSortOrder())
    private set
  var isGroupedByTags by mutableStateOf(presenter.isGroupedByTags)
    private set
  var isShowingRecents by mutableStateOf(presenter.isShowingRecents)
    private set
  var isDateShowing by mutableStateOf(presenter.isDateShowing)
    private set

  private val selection: SnapshotStateList<Int> = emptyList<Int>().toMutableStateList()
  val selectedIndices: Set<Int> get() = selection.toSet()
  val isSelecting: Boolean get() = selection.isNotEmpty()
  val selectedCount: Int get() = selection.size
  private val selectedRows: List<QuranRow> get() = selection.sorted().mapNotNull { rows.getOrNull(it) }

  /** Whether (edit tag, delete, tag bookmark) apply to the current selection. */
  val operations: BooleanArray get() = presenter.getContextualOperationsForItems(selectedRows)

  fun bind() = presenter.bind(this)
  fun unbind() = presenter.unbind(this)

  override fun onNewRawData(rawItems: BookmarkRawResult) {
    val result = converter.convertToUIResult(context, rawItems)
    selection.clear()
    showTags = presenter.shouldShowInlineTags()
    isDateShowing = presenter.isDateShowing
    tagMap = result.tagMap
    rows = result.rows
    isLoaded = true
  }

  fun changeSortOrder(order: Int) {
    presenter.setSortOrder(order)
    sortOrder = order
  }

  fun toggleGroupByTags() {
    presenter.toggleGroupByTags()
    isGroupedByTags = presenter.isGroupedByTags
  }

  fun toggleShowRecents() {
    presenter.toggleShowRecents()
    isShowingRecents = presenter.isShowingRecents
  }

  fun toggleShowDate() {
    presenter.toggleShowDate()
    isDateShowing = presenter.isDateShowing
  }

  fun clearSelection() = selection.clear()

  fun onRowClick(index: Int, row: QuranRow, actions: BookmarksActions) {
    if (isSelecting) {
      toggleSelected(index, row)
      return
    }

    when {
      // tapping a collection header collapses or expands its group in place; the header's
      // trailing chevron is what opens the collection as a screen
      row.isBookmarkHeader && row.isCollapsible -> {
        presenter.toggleCollectionCollapsed(row.tagId ?: return)
      }

      row.isHighlightsHeader -> presenter.toggleHighlightsCollapsed()
      row.isHighlightColor -> row.highlightColor?.let(actions.onOpenHighlightColor)
      !row.isHeader -> actions.onJumpTo(row)
    }
  }

  /**
   * The trailing chevron on a collection header opens that collection as its own screen. while
   * selecting, the chevron is just part of the row.
   */
  fun onOpenClick(index: Int, row: QuranRow, actions: BookmarksActions) {
    if (isSelecting) {
      toggleSelected(index, row)
    } else if (row.tagId != null) {
      actions.onOpenCollection(row)
    }
  }

  fun onRowLongClick(index: Int, row: QuranRow) {
    toggleSelected(index, row)
  }

  private fun toggleSelected(index: Int, row: QuranRow) {
    if (!(row.isBookmark || row.isEditableCollectionHeader)) return
    if (!selection.remove(index)) selection.add(index)
  }

  /** Deletes the selected rows after a delay (so it can be undone). Returns how many. */
  fun deleteSelected(): Int {
    val selected = selectedRows
    presenter.deleteAfterSomeTime(selected)
    selection.clear()
    return selected.size
  }

  fun undoDelete() {
    presenter.cancelDeletion()
    presenter.requestData(true)
  }

  fun editSelectedTag(actions: BookmarksActions) {
    val row = selectedRows.singleOrNull() ?: return
    row.tagId?.let { actions.onEditTag(it, row.text) }
    selection.clear()
  }

  fun tagSelectedBookmarks(actions: BookmarksActions) {
    val ids = selectedRows.mapNotNull { it.bookmarkId }.toTypedArray()
    actions.onTagBookmarks(ids)
    selection.clear()
  }
}
