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
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.fragment.compose.AndroidFragment
import com.quran.data.dao.BookmarkSortOrder
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.labs.androidquran.ui.fragment.BookmarksFragment
import com.quran.labs.androidquran.ui.fragment.JuzListFragment
import com.quran.labs.androidquran.ui.fragment.SuraListFragment
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
  val extraItems: List<HomeExtraItem> = emptyList()
)

class HomeExtraItem(@StringRes val titleResId: Int, val onClick: () -> Unit)

private val TabTitles = listOf(R.string.quran_sura, R.string.quran_juz2, R.string.menu_bookmarks)
private const val BOOKMARKS_TAB = 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(actions: HomeActions) {
  val pagerState = rememberPagerState(pageCount = { TabTitles.size })
  val scope = rememberCoroutineScope()
  var searching by rememberSaveable { mutableStateOf(false) }
  var bookmarksFragment by remember { mutableStateOf<BookmarksFragment?>(null) }

  BackHandler(enabled = searching) { searching = false }

  Scaffold(
    contentWindowInsets = WindowInsets.systemBars
      .union(WindowInsets.displayCutout)
      .only(WindowInsetsSides.Horizontal),
    topBar = {
      Column {
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
                Icon(QuranIcons.ArrowBack, contentDescription = stringResource(R.string.download_cancel))
              }
            }
          },
          actions = {
            if (!searching) {
              if (pagerState.currentPage == BOOKMARKS_TAB) {
                BookmarkOptionsMenu(bookmarksFragment)
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
        else -> AndroidFragment<BookmarksFragment>(onUpdate = { bookmarksFragment = it })
      }
    }
  }
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
      focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
      unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
    ),
    modifier = Modifier.focusRequester(focusRequester)
  )
  androidx.compose.runtime.LaunchedEffect(Unit) { focusRequester.requestFocus() }
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

/** Sort and display options for the bookmarks tab, which the fragment still owns. */
@Composable
private fun BookmarkOptionsMenu(fragment: BookmarksFragment?) {
  if (fragment == null) return
  var expanded by remember { mutableStateOf(false) }
  // the fragment's options aren't observable, so re-read them every time one is toggled
  var version by remember { mutableIntStateOf(0) }

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
        onClick = {
          onClick()
          version++
        }
      )
    }

    // reading `version` makes this block re-run after a toggle
    val sortOrder = remember(version) { fragment.sortOrder }
    Option(
      R.string.menu_sort_date,
      sortOrder == BookmarkSortOrder.SORT_DATE_ADDED
    ) { fragment.setSortOrder(BookmarkSortOrder.SORT_DATE_ADDED) }
    Option(
      R.string.menu_sort_location,
      sortOrder == BookmarkSortOrder.SORT_LOCATION
    ) { fragment.setSortOrder(BookmarkSortOrder.SORT_LOCATION) }
    Option(
      R.string.menu_sort_group_by_tags,
      remember(version) { fragment.isGroupedByTags }
    ) { fragment.toggleGroupByTags() }
    Option(
      R.string.menu_show_recents,
      remember(version) { fragment.isShowingRecents }
    ) { fragment.toggleShowRecents() }
    Option(
      R.string.menu_show_date,
      remember(version) { fragment.isDateShowing }
    ) { fragment.toggleShowDate() }
  }
}
