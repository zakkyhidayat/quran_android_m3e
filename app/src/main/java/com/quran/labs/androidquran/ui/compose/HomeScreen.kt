package com.quran.labs.androidquran.ui.compose

import androidx.activity.compose.BackHandler
import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.abs
import kotlin.math.floor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** What the home screen's app bar can trigger. The activity owns what each of these does. */
class HomeActions(
  val onSearch: (String) -> Unit,
  val resolveJump: (String) -> HomeJump?,
  val onLastPage: () -> Unit,
  /** Where else the continue button can take you: the reading bookmarks, the start of the surah. */
  val loadShortcuts: suspend () -> List<HomeJump> = { emptyList() },
  val onJumpToPage: () -> Unit,
  val onSettings: () -> Unit,
  val onHelp: () -> Unit,
  val onAbout: () -> Unit,
  val onSignIn: () -> Unit,
  val extraItems: List<HomeExtraItem> = emptyList()
)

/** A place the search bar can take you straight to, when what was typed is a page or an ayah. */
class HomeJump(val label: String, val go: () -> Unit)

class HomeExtraItem(@StringRes val titleResId: Int, val onClick: () -> Unit)

/**
 * The bar under the selected tab. It moves with your finger while you swipe between the tabs and
 * stretches out in the middle of the way, then settles at its tab.
 */
@Composable
private fun TabIndicator(
  pagerState: PagerState,
  tabCount: Int,
  labelWidths: Map<Int, Float>,
  modifier: Modifier = Modifier
) {
  val color = MaterialTheme.colorScheme.primary
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(4.dp)
      .drawBehind {
        val tabWidth = size.width / tabCount
        val progress = pagerState.currentPage + pagerState.currentPageOffsetFraction
        val within = progress - floor(progress)
        val stretch = 1f - abs(2f * within - 1f)
        // as wide as the label of the tab it is under, as the Material 3 primary tab asks
        val from = pagerState.currentPage.coerceIn(0, tabCount - 1)
        val to = (from + 1).coerceAtMost(tabCount - 1)
        val fromWidth = labelWidths[from] ?: 40.dp.toPx()
        val toWidth = labelWidths[to] ?: fromWidth
        val baseWidth = fromWidth + (toWidth - fromWidth) * within
        val width = baseWidth + 20.dp.toPx() * stretch
        val center = progress * tabWidth + tabWidth / 2f
        drawRoundRect(
          color = color,
          topLeft = Offset(center - width / 2f, 0f),
          size = Size(width, size.height),
          cornerRadius = CornerRadius(4.dp.toPx())
        )
      }
  )
}

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
  latestPageFlow: Flow<Int>,
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
    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
      // the list stays where it is (the top on a fresh start); the surah last read is highlighted
      suraState.onResume(latestPage)
    }
  }
  LaunchedEffect(suraState) {
    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
      latestPageFlow.collect { page ->
        // coming back from reading, wait a moment so the highlight is seen moving to its new
        // surah rather than already being there
        if (suraState.lastReadLoaded) delay(300)
        suraState.onLatestPage(page)
      }
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
      val jump = actions.resolveJump(query)
      if (jump != null) jump.go() else actions.onSearch(query.trim())
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
  val listPadding = PaddingValues(top = 4.dp, bottom = navigationBarPadding + 80.dp)
  // the button waits until it knows what to say and where the navigation bar is, so it appears
  // once, in its place, rather than in a wrong one for a split second
  var insetsSettled by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) {
    delay(250)
    insetsSettled = true
  }
  val fabReady = suraState.lastReadLoaded && (navigationBarPadding > 0.dp || insetsSettled)
  val fabLabel = if (suraState.lastReadPage != 0) {
    stringResource(
      R.string.continue_reading_page,
      QuranUtils.getLocalizedNumber(suraState.lastReadPage),
      QuranUtils.getLocalizedNumber(suraState.lastReadSura) + ":" +
        QuranUtils.getLocalizedNumber(suraState.lastReadAyah)
    )
  } else {
    stringResource(R.string.start_reading)
  }
  val fabDescription = if (suraState.lastReadPage != 0) {
    stringResource(R.string.continue_reading_description, fabLabel)
  } else {
    fabLabel
  }

  Scaffold(
    modifier = Modifier.nestedScroll(searchScrollBehavior.nestedScrollConnection),
    containerColor = MaterialTheme.colorScheme.surface,
    contentWindowInsets = WindowInsets.systemBars
      .union(WindowInsets.displayCutout)
      .only(WindowInsetsSides.Horizontal),
    snackbarHost = { SnackbarHost(snackbarHostState) },
    floatingActionButton = {
      // it arrives from the corner on a bouncy spring, and goes away quickly
      AnimatedVisibility(
        visible = !selecting && !isSearchOpen && fabReady,
        enter = fadeIn() +
          scaleIn(
            animationSpec = spring(dampingRatio = 0.5f, stiffness = 380f),
            initialScale = 0.4f,
            transformOrigin = TransformOrigin(1f, 1f)
          ) +
          slideInVertically(spring(dampingRatio = 0.6f, stiffness = 380f)) { it / 2 },
        exit = fadeOut() + scaleOut(targetScale = 0.6f, transformOrigin = TransformOrigin(1f, 1f))
      ) {
        // pressed, the button squares off and sinks, then springs back
        val fabSource = remember { MutableInteractionSource() }
        val fabPressed by fabSource.collectIsPressedAsState()
        val fabCorner by animateDpAsState(
          targetValue = if (fabPressed) 14.dp else 28.dp,
          animationSpec = spring(dampingRatio = 0.5f, stiffness = 520f),
          label = "fabCorner"
        )
        val fabScale by animateFloatAsState(
          targetValue = if (fabPressed) 0.94f else 1f,
          animationSpec = spring(dampingRatio = 0.45f, stiffness = 520f),
          label = "fabScale"
        )
        // a long press opens the other places it can take you
        var shortcuts by remember { mutableStateOf<List<HomeJump>>(emptyList()) }
        var shortcutsOpen by remember { mutableStateOf(false) }
        var longPressed by remember { mutableStateOf(false) }
        val haptics = LocalHapticFeedback.current
        LaunchedEffect(fabSource) {
          fabSource.interactions.collectLatest { interaction ->
            if (interaction is PressInteraction.Press) {
              delay(450)
              val found = actions.loadShortcuts()
              if (found.isNotEmpty()) {
                longPressed = true
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                shortcuts = found
                shortcutsOpen = true
              }
            }
          }
        }
        ExpressiveMenu(
          expanded = shortcutsOpen,
          onDismiss = { shortcutsOpen = false },
          sections = listOf(
            MenuSection(
              entries = shortcuts.map { jump -> MenuEntry(jump.label, HomeIcons.BookmarkFilled, onClick = jump.go) }
            )
          )
        )
        ExtendedFloatingActionButton(
          onClick = {
            if (longPressed) longPressed = false else actions.onLastPage()
          },
          shape = RoundedCornerShape(fabCorner),
          interactionSource = fabSource,
          icon = { Icon(QuranIcons.MenuBook, contentDescription = null) },
          text = {
            // the button is as wide as the longest label there can be (page 604, 114:6), so it
            // keeps the same width and place whatever is shown in it
            val longest = stringResource(
              R.string.continue_reading_page,
              QuranUtils.getLocalizedNumber(604),
              QuranUtils.getLocalizedNumber(114) + ":" + QuranUtils.getLocalizedNumber(6)
            )
            Box(contentAlignment = Alignment.Center) {
              Text(longest, maxLines = 1, modifier = Modifier.alpha(0f).clearAndSetSemantics {})
              Text(
                stringResource(R.string.start_reading),
                maxLines = 1,
                modifier = Modifier.alpha(0f).clearAndSetSemantics {}
              )
              // when the page you are on changes, the words roll up like a counter
              AnimatedContent(
                targetState = fabLabel,
                transitionSpec = {
                  (slideInVertically(spring(dampingRatio = 0.6f, stiffness = 500f)) { it } + fadeIn()) togetherWith
                    (slideOutVertically { -it } + fadeOut())
                },
                label = "fabLabel"
              ) { label -> Text(label, maxLines = 1) }
            }
          },
          modifier = Modifier
            .padding(bottom = navigationBarPadding)
            .graphicsLayer {
              scaleX = fabScale
              scaleY = fabScale
            }
            .semantics { contentDescription = fabDescription }
        )
      }
    },
    topBar = {
      // the status bar space belongs to this column, so the tabs stay clear of it once the search
      // bar has scrolled away
      Column(
        Modifier
          .background(MaterialTheme.colorScheme.surface)
          .windowInsetsPadding(WindowInsets.statusBars)
      ) {
        if (selecting) {
          SelectionBar(
            bookmarks = bookmarks,
            actions = bookmarkActions,
            onDelete = deleteSelected
          )
        } else {
          AppBarWithSearch(
            state = searchBarState,
            windowInsets = WindowInsets(0, 0, 0, 0),
            // the bar keeps its colors while it scrolls away, rather than shifting to a lighter tint
            colors = SearchBarDefaults.appBarWithSearchColors(
              appBarContainerColor = MaterialTheme.colorScheme.surface,
              scrolledAppBarContainerColor = MaterialTheme.colorScheme.surface,
              scrolledSearchBarContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
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
        val labelWidths = remember { mutableStateMapOf<Int, Float>() }
        Box {
        PrimaryTabRow(
          selectedTabIndex = pagerState.currentPage,
          containerColor = MaterialTheme.colorScheme.surface,
          // the indicator is drawn below, so it can follow the page while it is being swiped
          indicator = {}
        ) {
          TabTitles.forEachIndexed { index, titleResId ->
            val selectedTab = pagerState.currentPage == index
            Tab(
              selected = selectedTab,
              onClick = {
                scope.launch {
                  pagerState.animateScrollToPage(index, animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f))
                }
              },
              // the state layer is a rounded pill rather than a square
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp).clip(RoundedCornerShape(20.dp)),
              selectedContentColor = MaterialTheme.colorScheme.primary,
              unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
              Text(
                text = stringResource(titleResId),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selectedTab) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                onTextLayout = { labelWidths[index] = it.size.width.toFloat() },
                modifier = Modifier
                  .heightIn(min = 40.dp)
                  .wrapContentHeight(Alignment.CenterVertically)
              )
            }
          }
        }
        TabIndicator(pagerState, TabTitles.size, labelWidths, Modifier.align(Alignment.BottomStart))
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
          separateCards = true,
          highlightedSura = suraState.lastReadSura,
          listState = suraListState,
          contentPadding = listPadding,
          onRowClick = { _, row -> if (row.page != 0) onRowClick(row) }
        )

        1 -> QuranRowList(
          rows = juzState.rows,
          separateCards = true,
          listState = juzListState,
          contentPadding = listPadding,
          onRowClick = { _, row -> if (row.page != 0) onRowClick(row) }
        )

        2 -> QuranRowList(
          rows = hizbState.rows,
          separateCards = true,
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
  ExpandedFullScreenSearchBar(state = searchBarState, inputField = searchField) {
    SearchHelp(
      query = searchTextState.text.toString(),
      jump = actions.resolveJump(searchTextState.text.toString()),
      onJump = { submitSearch(searchTextState.text.toString()) },
      onWords = {
        val words = searchTextState.text.toString().trim()
        scope.launch { searchBarState.animateToCollapsed() }
        actions.onSearch(words)
        searchTextState.clearText()
      }
    )
  }
}

/**
 * What the expanded search bar shows under the field: a short explanation of the two things it
 * does (find words, jump to a page or ayah), then the jump or the search for what was typed.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SearchHelp(query: String, jump: HomeJump?, onJump: () -> Unit, onWords: () -> Unit) {
  Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    if (jump != null) {
      SearchHelpRow(
        icon = HomeIcons.Numbers,
        title = jump.label,
        summary = stringResource(R.string.search_jump_summary),
        highlighted = true,
        onClick = onJump
      )
    }
    if (query.isNotBlank()) {
      SearchHelpRow(
        icon = QuranIcons.Search,
        title = stringResource(R.string.search_for_query, query.trim()),
        summary = stringResource(R.string.search_words_summary),
        highlighted = jump == null,
        onClick = onWords
      )
    } else {
      SearchHelpRow(
        icon = QuranIcons.Search,
        title = stringResource(R.string.search_help_words_title),
        summary = stringResource(R.string.search_words_summary)
      )
      SearchHelpRow(
        icon = HomeIcons.Numbers,
        title = stringResource(R.string.search_help_jump_title),
        summary = stringResource(R.string.search_help_jump_summary)
      )
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SearchHelpRow(
  icon: ImageVector,
  title: String,
  summary: String,
  highlighted: Boolean = false,
  onClick: (() -> Unit)? = null
) {
  SegmentedListItem(
    onClick = onClick ?: {},
    shapes = ListItemDefaults.shapes(shape = RoundedCornerShape(24.dp)),
    colors = ListItemDefaults.segmentedColors(
      containerColor = if (highlighted) {
        MaterialTheme.colorScheme.primaryContainer
      } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
      }
    ),
    leadingContent = { Icon(icon, contentDescription = null) },
    supportingContent = { Text(summary) }
  ) {
    Text(title, style = MaterialTheme.typography.titleMedium)
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
    windowInsets = WindowInsets(0, 0, 0, 0),
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
  ExpressiveIconButton(
    onClick = { expanded = true },
    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
  ) {
    Icon(
      HomeIcons.MoreVert,
      contentDescription = stringResource(androidx.appcompat.R.string.abc_action_menu_overflow_description)
    )
  }
  Spacer(Modifier.width(8.dp))
  val main = listOf(
    MenuEntry(stringResource(R.string.menu_settings), HomeIcons.Settings, onClick = actions.onSettings)
  )
  val info = listOf(
    MenuEntry(stringResource(R.string.menu_help), HomeIcons.Help, onClick = actions.onHelp),
    MenuEntry(stringResource(R.string.menu_about), HomeIcons.Info, onClick = actions.onAbout)
  ) + actions.extraItems.map { MenuEntry(stringResource(it.titleResId), onClick = it.onClick) }
  ExpressiveMenu(
    expanded = expanded,
    onDismiss = { expanded = false },
    sections = listOf(MenuSection(entries = main), MenuSection(entries = info))
  )
}

/** Sort and display options for the bookmarks tab. */
@Composable
private fun BookmarkOptionsMenu(bookmarks: BookmarksState) {
  var expanded by remember { mutableStateOf(false) }

  ExpressiveIconButton(
    onClick = { expanded = true },
    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
  ) {
    Icon(HomeIcons.Sort, contentDescription = stringResource(R.string.menu_sort))
  }
  Spacer(Modifier.width(6.dp))
  ExpressiveMenu(
    expanded = expanded,
    onDismiss = { expanded = false },
    sections = listOf(
      MenuSection(
        title = stringResource(R.string.menu_sort),
        entries = listOf(
          MenuEntry(
            stringResource(R.string.menu_sort_date),
            selected = bookmarks.sortOrder == BookmarkSortOrder.SORT_DATE_ADDED
          ) { bookmarks.changeSortOrder(BookmarkSortOrder.SORT_DATE_ADDED) },
          MenuEntry(
            stringResource(R.string.menu_sort_location),
            selected = bookmarks.sortOrder == BookmarkSortOrder.SORT_LOCATION
          ) { bookmarks.changeSortOrder(BookmarkSortOrder.SORT_LOCATION) }
        )
      ),
      MenuSection(
        entries = listOf(
          MenuEntry(
            stringResource(R.string.menu_sort_group_by_tags),
            selected = bookmarks.isGroupedByTags,
            keepOpen = true,
            onClick = bookmarks::toggleGroupByTags
          ),
          MenuEntry(
            stringResource(R.string.menu_show_recents),
            selected = bookmarks.isShowingRecents,
            keepOpen = true,
            onClick = bookmarks::toggleShowRecents
          ),
          MenuEntry(
            stringResource(R.string.menu_show_date),
            selected = bookmarks.isDateShowing,
            keepOpen = true,
            onClick = bookmarks::toggleShowDate
          )
        )
      )
    )
  )
}
