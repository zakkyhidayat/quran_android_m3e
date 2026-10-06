package com.quran.labs.androidquran.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quran.data.dao.BookmarkSortOrder
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.labs.androidquran.ui.helpers.QuranRow
import com.quran.labs.androidquran.util.QuranUtils

/** What the bookmark list screen (one collection, or one highlight color) can trigger. */
class BookmarkListActions(
  val onBack: () -> Unit,
  val onRowClick: (Int, QuranRow) -> Unit,
  val onRowLongClick: (Int, QuranRow) -> Unit,
  val onClearSelection: () -> Unit,
  val onSortOrder: (Int) -> Unit,
  val onRename: () -> Unit,
  val onTag: () -> Unit,
  val onDelete: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BookmarkListScreen(
  title: String,
  rows: List<QuranRow>,
  selectedIndices: Set<Int>,
  sortOrder: Int,
  canRename: Boolean,
  canTag: Boolean,
  emptyText: String,
  snackbarHostState: SnackbarHostState,
  actions: BookmarkListActions
) {
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  val selecting = selectedIndices.isNotEmpty()
  val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    containerColor = MaterialTheme.colorScheme.surface,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      if (selecting) {
        TopAppBar(
          title = { Text(QuranUtils.getLocalizedNumber(selectedIndices.size)) },
          navigationIcon = {
            IconButton(onClick = actions.onClearSelection) {
              Icon(QuranIcons.Close, contentDescription = stringResource(R.string.download_cancel))
            }
          },
          actions = {
            if (canTag) {
              IconButton(onClick = actions.onTag) {
                Icon(HomeIcons.Label, contentDescription = stringResource(R.string.tag_bookmark))
              }
            }
            IconButton(onClick = actions.onDelete) {
              Icon(HomeIcons.Delete, contentDescription = stringResource(R.string.delete_tag))
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
          )
        )
      } else {
        LargeFlexibleTopAppBar(
          title = { Text(title, maxLines = 1) },
          navigationIcon = {
            IconButton(onClick = actions.onBack) {
              Icon(QuranIcons.ArrowBack, contentDescription = stringResource(R.string.menu_back_to_page))
            }
          },
          actions = {
            if (canRename) {
              IconButton(onClick = actions.onRename) {
                Icon(HomeIcons.Edit, contentDescription = stringResource(R.string.edit_tag))
              }
            }
            SortMenu(sortOrder, actions.onSortOrder)
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
          ),
          scrollBehavior = scrollBehavior
        )
      }
    }
  ) { innerPadding ->
    Box(
      Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      if (rows.isEmpty()) {
        Text(
          text = emptyText,
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier = Modifier
            .align(Alignment.Center)
            .padding(horizontal = 32.dp)
        )
      } else {
        QuranRowList(
          rows = rows,
          isEditable = true,
          separateCards = true,
          selectedIndices = selectedIndices,
          contentPadding = PaddingValues(top = 4.dp, bottom = navigationBarPadding + 16.dp),
          onRowLongClick = actions.onRowLongClick,
          onRowClick = actions.onRowClick
        )
      }
    }
  }
}

@Composable
private fun SortMenu(sortOrder: Int, onSortOrder: (Int) -> Unit) {
  var expanded by remember { mutableStateOf(false) }
  IconButton(onClick = { expanded = true }) {
    Icon(HomeIcons.Sort, contentDescription = stringResource(R.string.menu_sort))
  }
  ExpressiveMenu(
    expanded = expanded,
    onDismiss = { expanded = false },
    sections = listOf(
      MenuSection(
        title = stringResource(R.string.menu_sort),
        entries = listOf(
          MenuEntry(
            stringResource(R.string.menu_sort_date),
            selected = sortOrder == BookmarkSortOrder.SORT_DATE_ADDED
          ) { onSortOrder(BookmarkSortOrder.SORT_DATE_ADDED) },
          MenuEntry(
            stringResource(R.string.menu_sort_location),
            selected = sortOrder == BookmarkSortOrder.SORT_LOCATION
          ) { onSortOrder(BookmarkSortOrder.SORT_LOCATION) }
        )
      )
    )
  )
}
