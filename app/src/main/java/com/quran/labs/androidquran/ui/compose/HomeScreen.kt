package com.quran.labs.androidquran.ui.compose

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.quran.data.dao.BookmarkSortOrder
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.labs.androidquran.data.Constants
import com.quran.labs.androidquran.presenter.bookmark.BookmarkPresenter
import com.quran.labs.androidquran.ui.helpers.QuranRow
import com.quran.labs.androidquran.util.QuranUtils
import com.quran.mobile.feature.sync.QuranSyncManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** What the home screen's app bar can trigger. The activity owns what each of these does. */
class HomeActions(
  val onSearch: (String) -> Unit,
  val onLastPage: () -> Unit,
  val onJumpToPage: () -> Unit,
  val onSettings: () -> Unit,
  val onHelp: () -> Unit,
  val onAbout: () -> Unit,
  val onOtherApps: () -> Unit,
  val onSignIn: () -> Unit,
  val extraItems: List<HomeExtraItem> = emptyList()
)

class HomeExtraItem(@StringRes val titleResId: Int, val onClick: () -> Unit)

private val TabTitles = listOf(
  R.string.quran_sura,
  R.string.quran_juz2,
  R.string.quran_hizb,
  R.string.menu_bookmarks
)
private const val BOOKMARKS_TAB = 3

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
  actions: HomeActions,
  suraState: SuraListState,
  juzState: JuzListState,
  hizbState: HizbListState,
  bookmarks: BookmarksState,
  bookmarkActions: BookmarksActions,
  syncManager: QuranSyncManager,
  latestPage: suspend () -> Int,
  onRowClick: (QuranRow) -> Unit
) {
  val context = LocalContext.current
  val pagerState = rememberPagerState(pageCount = { TabTitles.size })
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val selecting = bookmarks.isSelecting

  val searchBarState = rememberSearchBarState()
  val searchTextState = rememberTextFieldState()
  val searchScrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
  val isSearchOpen = searchBarState.currentValue == SearchBarValue.Expanded

  // the lists live here, not in their tabs, so a tab keeps its scroll position while off screen
  val suraListState = rememberLazyListState()
  val juzListState = rememberLazyListState()
  val hizbListState = rememberLazyListState()
  val lifecycle = LocalLifecycleOwner.current.lifecycle

  LaunchedEffect(suraState) {
    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { suraState.observeReadingBookmarks() }
  }
  LaunchedEffect(suraState) {
    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
      suraState.onResume(latestPage)?.let { suraListState.scrollToItem(it) }
    }
  }
  LaunchedEffect(juzState) {
    juzState.load()
    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
      val page = latestPage()
      if (page != Constants.NO_PAGE) juzListState.scrollToItem(juzState.positionFor(page))
    }
  }
  LaunchedEffect(hizbState) {
    hizbState.load()
    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
      val page = latestPage()
      if (page != Constants.NO_PAGE) hizbListState.scrollToItem(hizbState.positionFor(page))
    }
  }

  BackHandler(enabled = isSearchOpen) { scope.launch { searchBarState.animateToCollapsed() } }
  BackHandler(enabled = selecting) { bookmarks.clearSelection() }
  // selection belongs to the bookmarks tab, so swiping to another tab drops it
  LaunchedEffect(pagerState.currentPage) {
    if (pagerState.currentPage != BOOKMARKS_TAB) bookmarks.clearSelection()
  }

  val deleteSelected: () -> Unit = {
    val count = bookmarks.deleteSelected()
    val message = context.resources.getQuantityString(R.plurals.bookmark_tag_deleted, count, count)
    val undo = context.getString(R.string.undo)
    scope.launch {
      // an indefinite snackbar that this timer dismisses, since the delay before the deletion is
      // really applied is the presenter's, not one of the standard snackbar durations
      val dismisser = launch {
        delay(BookmarkPresenter.DELAY_DELETION_DURATION_IN_MS.toLong())
        snackbarHostState.currentSnackbarData?.dismiss()
      }
      val result = snackbarHostState.showSnackbar(
        message = message,
        actionLabel = undo,
        duration = SnackbarDuration.Indefinite
      )
      dismisser.cancel()
      if (result == SnackbarResult.ActionPerformed) bookmarks.undoDelete()
    }
  }

  val submitSearch: (String) -> Unit = { query ->
    if (query.isNotBlank()) {
      scope.launch { searchBarState.animateToCollapsed() }
      actions.onSearch(query.trim())
      // the results open in their own screen, so the bar should be empty when you come back
      searchTextState.clearText()
    }
  }
  val searchField: @Composable () -> Unit = {
    SearchBarDefaults.InputField(
      textFieldState = searchTextState,
      searchBarState = searchBarState,
      onSearch = submitSearch,
      placeholder = { Text(stringResource(R.string.search_hint)) },
      leadingIcon = {
        Icon(QuranIcons.Search, contentDescription = stringResource(R.string.menu_search))
      }
    )
  }

  val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
  // leave room under the lists for the continue-reading button
  val listPadding = PaddingValues(top = 4.dp, bottom = navigationBarPadding + 96.dp)

  Scaffold(
    modifier = Modifier.nestedScroll(searchScrollBehavior.nestedScrollConnection),
    containerColor = MaterialTheme.colorScheme.surface,
    contentWindowInsets = WindowInsets.systemBars
      .union(WindowInsets.displayCutout)
      .only(WindowInsetsSides.Horizontal),
    snackbarHost = { SnackbarHost(snackbarHostState) },
    floatingActionButton = {
      if (!selecting && !isSearchOpen) {
        ExtendedFloatingActionButton(
          onClick = actions.onLastPage,
          icon = { Icon(QuranIcons.MenuBook, contentDescription = null) },
          text = { Text(stringResource(R.string.menu_jump_last_page)) },
          modifier = Modifier.padding(bottom = navigationBarPadding)
        )
      }
    },
    topBar = {
      Column {
        if (selecting) {
          SelectionBar(
            bookmarks = bookmarks,
            actions = bookmarkActions,
            onDelete = deleteSelected
          )
        } else {
          AppBarWithSearch(
            state = searchBarState,
            inputField = searchField,
            actions = {
              if (pagerState.currentPage == BOOKMARKS_TAB) {
                BookmarkOptionsMenu(bookmarks)
              }
              OverflowMenu(actions)
            },
            scrollBehavior = searchScrollBehavior
          )
        }
        PrimaryScrollableTabRow(
          selectedTabIndex = pagerState.currentPage,
          edgePadding = 12.dp,
          containerColor = MaterialTheme.colorScheme.surface
        ) {
          TabTitles.forEachIndexed { index, titleResId ->
            Tab(
              selected = pagerState.currentPage == index,
              onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
              text = { Text(stringResource(titleResId), maxLines = 1) }
            )
          }
        }
      }
    }
  ) { innerPadding ->
    HorizontalPager(
      state = pagerState,
      beyondViewportPageCount = 1,
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) { page ->
      when (page) {
        0 -> QuranRowList(
          rows = suraState.rows,
          listState = suraListState,
          contentPadding = listPadding,
          onRowClick = { _, row -> if (row.page != 0) onRowClick(row) }
        )

        1 -> QuranRowList(
          rows = juzState.rows,
          listState = juzListState,
          contentPadding = listPadding,
          onRowClick = { _, row -> if (row.page != 0) onRowClick(row) }
        )

        2 -> QuranRowList(
          rows = hizbState.rows,
          listState = hizbListState,
          contentPadding = listPadding,
          onRowClick = { _, row -> if (row.page != 0) onRowClick(row) }
        )

        else -> BookmarksTab(
          state = bookmarks,
          actions = bookmarkActions,
          syncManager = syncManager,
          onSignIn = actions.onSignIn,
          contentPadding = listPadding
        )
      }
    }
  }

  // the search bar grows to fill the screen when tapped; results open in the search screen
  ExpandedFullScreenSearchBar(state = searchBarState, inputField = searchField) {}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionBar(
  bookmarks: BookmarksState,
  actions: BookmarksActions,
  onDelete: () -> Unit
) {
  val operations = bookmarks.operations
  TopAppBar(
    title = { Text(QuranUtils.getLocalizedNumber(bookmarks.selectedCount)) },
    navigationIcon = {
      IconButton(onClick = bookmarks::clearSelection) {
        Icon(QuranIcons.Close, contentDescription = stringResource(R.string.download_cancel))
      }
    },
    actions = {
      if (operations[2]) {
        IconButton(onClick = { bookmarks.tagSelectedBookmarks(actions) }) {
          Icon(HomeIcons.Label, contentDescription = stringResource(R.string.tag_bookmark))
        }
      }
      if (operations[0]) {
        IconButton(onClick = { bookmarks.editSelectedTag(actions) }) {
          Icon(HomeIcons.Edit, contentDescription = stringResource(R.string.edit_tag))
        }
      }
      if (operations[1]) {
        IconButton(onClick = onDelete) {
          Icon(HomeIcons.Delete, contentDescription = stringResource(R.string.delete_tag))
        }
      }
      IconButton(onClick = actions.onAddTag) {
        Icon(HomeIcons.Add, contentDescription = stringResource(R.string.new_tag))
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.secondaryContainer,
      titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
      navigationIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
      actionIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
    )
  )
}

@Composable
private fun OverflowMenu(actions: HomeActions) {
  var expanded by remember { mutableStateOf(false) }
  IconButton(onClick = { expanded = true }) {
    Icon(HomeIcons.MoreVert, contentDescription = null)
  }
  DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
    @Composable
    fun Item(@StringRes title: Int, onClick: () -> Unit) {
      DropdownMenuItem(
        text = { Text(stringResource(title)) },
        onClick = {
          expanded = false
          onClick()
        }
      )
    }

    Item(R.string.menu_jump, actions.onJumpToPage)
    Item(R.string.menu_settings, actions.onSettings)
    Item(R.string.menu_help, actions.onHelp)
    Item(R.string.menu_about, actions.onAbout)
    Item(R.string.menu_other_apps, actions.onOtherApps)
    actions.extraItems.forEach { Item(it.titleResId, it.onClick) }
  }
}

/** Sort and display options for the bookmarks tab. */
@Composable
private fun BookmarkOptionsMenu(bookmarks: BookmarksState) {
  var expanded by remember { mutableStateOf(false) }

  IconButton(onClick = { expanded = true }) {
    Icon(HomeIcons.Sort, contentDescription = stringResource(R.string.menu_sort))
  }
  DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
    @Composable
    fun Option(@StringRes title: Int, checked: Boolean, onClick: () -> Unit) {
      DropdownMenuItem(
        text = { Text(stringResource(title)) },
        leadingIcon = {
          if (checked) Icon(QuranIcons.Check, contentDescription = null)
        },
        onClick = onClick
      )
    }

    Option(
      R.string.menu_sort_date,
      bookmarks.sortOrder == BookmarkSortOrder.SORT_DATE_ADDED
    ) { bookmarks.changeSortOrder(BookmarkSortOrder.SORT_DATE_ADDED) }
    Option(
      R.string.menu_sort_location,
      bookmarks.sortOrder == BookmarkSortOrder.SORT_LOCATION
    ) { bookmarks.changeSortOrder(BookmarkSortOrder.SORT_LOCATION) }
    Option(R.string.menu_sort_group_by_tags, bookmarks.isGroupedByTags, bookmarks::toggleGroupByTags)
    Option(R.string.menu_show_recents, bookmarks.isShowingRecents, bookmarks::toggleShowRecents)
    Option(R.string.menu_show_date, bookmarks.isDateShowing, bookmarks::toggleShowDate)
  }
}
