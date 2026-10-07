package com.quran.labs.androidquran.ui.compose

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import kotlinx.coroutines.launch

/** One match: where it is, the text with the matched words marked up as html, and its location. */
class SearchResult(val sura: Int, val ayah: Int, val html: String, val location: String)

/**
 * The search results screen: a search bar (type a new query here), a line saying how many matches
 * there were, an optional warning with a button to fix what's missing, then the matches.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchScreen(
  query: String,
  summary: String?,
  warning: String?,
  actionLabel: String?,
  results: List<SearchResult>,
  onSearch: (String) -> Unit,
  onAction: () -> Unit,
  onResultClick: (SearchResult) -> Unit,
  onBack: () -> Unit
) {
  val scope = rememberCoroutineScope()
  val searchBarState = rememberSearchBarState()
  val textState = rememberTextFieldState(query)
  val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
  val isOpen = searchBarState.currentValue == SearchBarValue.Expanded

  // a search started elsewhere (from the home screen) arrives as a new query
  LaunchedEffect(query) { textState.setTextAndPlaceCursorAtEnd(query) }
  BackHandler(enabled = isOpen) { scope.launch { searchBarState.animateToCollapsed() } }

  val submit: (String) -> Unit = { text ->
    if (text.isNotBlank()) {
      scope.launch { searchBarState.animateToCollapsed() }
      onSearch(text.trim())
    }
  }
  val inputField: @Composable () -> Unit = {
    SearchBarDefaults.InputField(
      textFieldState = textState,
      searchBarState = searchBarState,
      onSearch = submit,
      placeholder = { Text(stringResource(R.string.search_hint)) },
      leadingIcon = {
        IconButton(onClick = onBack) {
          Icon(QuranIcons.ArrowBack, contentDescription = stringResource(R.string.menu_back_to_page))
        }
      }
    )
  }

  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    containerColor = MaterialTheme.colorScheme.surface,
    contentWindowInsets = WindowInsets.systemBars
      .union(WindowInsets.displayCutout)
      .only(WindowInsetsSides.Horizontal),
    topBar = {
      AppBarWithSearch(
        state = searchBarState,
        inputField = inputField,
        scrollBehavior = scrollBehavior
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      if (warning != null) {
        Surface(
          shape = MaterialTheme.shapes.large,
          color = MaterialTheme.colorScheme.secondaryContainer,
          contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
          Text(
            text = warning,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(16.dp)
          )
        }
      }
      if (actionLabel != null) {
        FilledTonalButton(
          onClick = onAction,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
          Text(actionLabel)
        }
      }
      if (summary != null) {
        Text(
          text = summary,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 16.dp, bottom = 8.dp)
        )
      }
      ResultsList(results, onResultClick)
    }
  }

  ExpandedFullScreenSearchBar(state = searchBarState, inputField = inputField) {}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ResultsList(results: List<SearchResult>, onResultClick: (SearchResult) -> Unit) {
  val highlight = MaterialTheme.colorScheme.primary
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(
      bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
    ),
    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
  ) {
    itemsIndexed(results) { index, result ->
      // the matched words come back bold; color them too so they stand out in the long text
      val text = remember(result, highlight) {
        val parsed = AnnotatedString.fromHtml(result.html)
        AnnotatedString(
          text = parsed.text,
          spanStyles = parsed.spanStyles.map { it.copy(item = it.item.copy(color = highlight)) },
          paragraphStyles = parsed.paragraphStyles
        )
      }
      SegmentedListItem(
        onClick = { onResultClick(result) },
        shapes = ListItemDefaults.segmentedShapes(index, results.size),
        colors = ListItemDefaults.segmentedColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier.padding(horizontal = 16.dp).expressiveAppear(rise = 20f, from = 0.97f),
        supportingContent = { Text(result.location) }
      ) {
        Text(
          text = text,
          style = MaterialTheme.typography.bodyLarge,
          maxLines = 4,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}
