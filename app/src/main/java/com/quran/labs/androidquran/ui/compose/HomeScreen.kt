package com.quran.labs.androidquran.ui.compose

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.PrimaryTabRow
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
import androidx.fragment.compose.AndroidFragment
import com.quran.data.dao.BookmarkSortOrder
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.labs.androidquran.presenter.bookmark.BookmarkPresenter
import com.quran.labs.androidquran.ui.fragment.JuzListFragment
import com.quran.labs.androidquran.ui.fragment.SuraListFragment
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

private val TabTitles = listOf(R.string.quran_sura, R.string.quran_juz2, R.string.menu_bookmarks)
private const val BOOKMARKS_TAB = 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  actions: HomeActions,
  bookmarks: BookmarksState,
  bookmarkActions: BookmarksActions,
  syncManager: QuranSyncManager
) {
  val context = LocalContext.current
  val pagerState = rememberPagerState(pageCount = { TabTitles.size })
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  var searching by rememberSaveable { mutableStateOf(false) }
  val selecting = bookmarks.isSelecting

  BackHandler(enabled = searching) { searching = false }
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

  Scaffold(
    contentWindowInsets = WindowInsets.systemBars
      .union(WindowInsets.displayCutout)
      .only(WindowInsetsSides.Horizontal),
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      Column {
        if (selecting) {
          SelectionBar(
            bookmarks = bookmarks,
            actions = bookmarkActions,
            onDelete = deleteSelected
          )
        } else {
          TopAppBar(
            title = {
              if (searching) {
                SearchField(onSearch = {
                  searching = false
                  actions.onSearch(it)
                })
              } else {
                Text(stringResource(R.string.app_name))
              }
            },
            navigationIcon = {
              if (searching) {
                IconButton(onClick = { searching = false }) {
                  Icon(
                    QuranIcons.ArrowBack,
                    contentDescription = stringResource(R.string.download_cancel)
                  )
                }
              }
            },
            actions = {
              if (!searching) {
                if (pagerState.currentPage == BOOKMARKS_TAB) {
                  BookmarkOptionsMenu(bookmarks)
                }
                IconButton(onClick = { searching = true }) {
                  Icon(QuranIcons.Search, contentDescription = stringResource(R.string.menu_search))
                }
                IconButton(onClick = actions.onLastPage) {
                  Icon(
                    QuranIcons.MenuBook,
                    contentDescription = stringResource(R.string.menu_jump_last_page)
                  )
                }
                OverflowMenu(actions)
              }
            },
            colors = TopAppBarDefaults.topAppBarColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
          )
        }
        PrimaryTabRow(
          selectedTabIndex = pagerState.currentPage,
          containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
          TabTitles.forEachIndexed { index, titleResId ->
            Tab(
              selected = pagerState.currentPage == index,
              onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
              text = { Text(stringResource(titleResId)) }
            )
          }
        }
      }
    }
  ) { innerPadding ->
    HorizontalPager(
      state = pagerState,
      beyondViewportPageCount = TabTitles.size,
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) { page ->
      when (page) {
        0 -> AndroidFragment<SuraListFragment>()
        1 -> AndroidFragment<JuzListFragment>()
        else -> BookmarksTab(
          state = bookmarks,
          actions = bookmarkActions,
          syncManager = syncManager,
          onSignIn = actions.onSignIn
        )
      }
    }
  }
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
private fun SearchField(onSearch: (String) -> Unit) {
  var query by rememberSaveable { mutableStateOf("") }
  val focusRequester = remember { FocusRequester() }
  TextField(
    value = query,
    onValueChange = { query = it },
    singleLine = true,
    placeholder = { Text(stringResource(R.string.search_hint)) },
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    keyboardActions = KeyboardActions(onSearch = { if (query.isNotBlank()) onSearch(query.trim()) }),
    colors = TextFieldDefaults.colors(
      focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
      unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
      focusedIndicatorColor = Color.Transparent,
      unfocusedIndicatorColor = Color.Transparent
    ),
    modifier = Modifier.focusRequester(focusRequester)
  )
  LaunchedEffect(Unit) { focusRequester.requestFocus() }
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
