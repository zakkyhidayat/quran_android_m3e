package com.quran.labs.androidquran.ui.compose

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.labs.androidquran.dao.translation.TranslationItem

/** What the translation manager can trigger. The activity owns what each of these does. */
class TranslationManagerActions(
  val onBack: () -> Unit,
  val onRefresh: () -> Unit,
  val onDownload: (TranslationItem) -> Unit,
  val onMove: (TranslationItem, Int) -> Unit,
  val onRemove: (TranslationItem) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TranslationManagerScreen(
  items: List<TranslationItem>,
  downloadingId: Int?,
  refreshing: Boolean,
  snackbarHostState: SnackbarHostState,
  actions: TranslationManagerActions
) {
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
  var query by remember { mutableStateOf("") }
  val downloaded = remember(items) { items.filter { it.exists() }.sortedBy { it.displayOrder } }
  val available = remember(items, query) { items.filter { !it.exists() && it.matches(query) } }
  var confirmRemoval by remember { mutableStateOf<TranslationItem?>(null) }

  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    containerColor = MaterialTheme.colorScheme.surface,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      LargeFlexibleTopAppBar(
        title = { Text(stringResource(R.string.prefs_translations)) },
        navigationIcon = {
          IconButton(onClick = actions.onBack) {
            Icon(QuranIcons.ArrowBack, contentDescription = stringResource(R.string.menu_back_to_page))
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
          scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        scrollBehavior = scrollBehavior
      )
    }
  ) { innerPadding ->
    Column(
      Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
    TranslationFilterField(query, { query = it }, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    PullToRefreshBox(
      isRefreshing = refreshing,
      onRefresh = actions.onRefresh,
      modifier = Modifier.fillMaxSize()
    ) {
      TranslationList(
        downloaded = downloaded,
        available = available,
        downloadingId = downloadingId,
        contentPadding = PaddingValues(top = 4.dp, bottom = navigationBarPadding + 16.dp),
        onDownload = actions.onDownload,
        onMove = actions.onMove,
        onRemove = { confirmRemoval = it }
      )
    }
    }
  }

  confirmRemoval?.let { item ->
    AlertDialog(
      onDismissRequest = { confirmRemoval = null },
      title = { Text(stringResource(R.string.remove_dlg_title)) },
      text = { Text(stringResource(R.string.remove_dlg_msg, item.name())) },
      confirmButton = {
        TextButton(onClick = {
          confirmRemoval = null
          actions.onRemove(item)
        }) { Text(stringResource(com.quran.mobile.common.ui.core.R.string.remove_button)) }
      },
      dismissButton = {
        TextButton(onClick = { confirmRemoval = null }) {
          Text(stringResource(com.quran.mobile.common.ui.core.R.string.cancel))
        }
      }
    )
  }
}

/**
 * The translations you have, which you can put in order or remove, and the ones you can get. Also
 * used by the first-run setup, where [onMove] and [onRemove] are not needed.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TranslationList(
  downloaded: List<TranslationItem>,
  available: List<TranslationItem>,
  downloadingId: Int?,
  contentPadding: PaddingValues,
  modifier: Modifier = Modifier,
  onDownload: (TranslationItem) -> Unit,
  onMove: ((TranslationItem, Int) -> Unit)? = null,
  onRemove: ((TranslationItem) -> Unit)? = null,
  header: (LazyListScope.() -> Unit)? = null
) {
  val listState = rememberLazyListState()
  // the translation being dragged to a new place, and how far it has been dragged
  var draggingKey by remember { mutableStateOf<String?>(null) }
  var dragOffset by remember { mutableFloatStateOf(0f) }

  LazyColumn(
    state = listState,
    modifier = modifier.fillMaxSize(),
    contentPadding = contentPadding,
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    header?.invoke(this)
    if (downloaded.isNotEmpty()) {
      item(key = "header-downloaded") { ListHeader(stringResource(R.string.downloaded_translations)) }
      items(downloaded.size, key = { "d-${downloaded[it].translation.id}" }) { index ->
        val item = downloaded[index]
        val key = "d-${item.translation.id}"
        val dragged = draggingKey == key
        TranslationCard(
          item = item,
          downloading = downloadingId == item.translation.id,
          onClick = { if (item.needsUpgrade()) onDownload(item) },
          modifier = if (dragged) {
            Modifier
              .zIndex(1f)
              .graphicsLayer { translationY = dragOffset }
          } else {
            Modifier.animateItem()
          }
        ) {
          if (item.needsUpgrade() && downloadingId != item.translation.id) {
            IconButton(onClick = { onDownload(item) }) {
              Icon(HomeIcons.Download, contentDescription = stringResource(R.string.update_available))
            }
          }
          if (onMove != null && downloaded.size > 1) {
            val currentDownloaded by rememberUpdatedState(downloaded)
            Icon(
              HomeIcons.DragHandle,
              contentDescription = stringResource(R.string.translation_drag_to_reorder),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier
                .size(48.dp)
                .padding(12.dp)
                .pointerInput(key) {
                  detectDragGestures(
                    onDragStart = {
                      draggingKey = key
                      dragOffset = 0f
                    },
                    onDragEnd = { draggingKey = null; dragOffset = 0f },
                    onDragCancel = { draggingKey = null; dragOffset = 0f },
                    onDrag = { change, amount ->
                      change.consume()
                      dragOffset += amount.y
                      val visible = listState.layoutInfo.visibleItemsInfo
                      val own = visible.firstOrNull { it.key == key } ?: return@detectDragGestures
                      val center = own.offset + own.size / 2f + dragOffset
                      val target = visible.firstOrNull {
                        it.key != key && (it.key as? String)?.startsWith("d-") == true &&
                          center >= it.offset && center < it.offset + it.size
                      }
                      if (target != null) {
                        val list = currentDownloaded
                        val from = list.indexOfFirst { "d-${it.translation.id}" == key }
                        val to = list.indexOfFirst { "d-${it.translation.id}" == target.key }
                        if (from >= 0 && to >= 0 && from != to) {
                          onMove(list[from], to - from)
                          // the dragged card takes the target's place, so the finger stays on it
                          dragOffset += own.offset - target.offset
                        }
                      }
                    }
                  )
                }
            )
          }
          if (onRemove != null) {
            IconButton(onClick = { onRemove(item) }) {
              Icon(
                HomeIcons.Delete,
                contentDescription = stringResource(com.quran.mobile.common.ui.core.R.string.remove_button)
              )
            }
          }
        }
      }
    }
    item(key = "header-available") { ListHeader(stringResource(R.string.available_translations)) }
    items(available, key = { "a-${it.translation.id}" }) { item ->
      TranslationCard(
        item = item,
        downloading = downloadingId == item.translation.id,
        onClick = { onDownload(item) }
      ) {
        if (downloadingId != item.translation.id) {
          IconButton(onClick = { onDownload(item) }) {
            Icon(HomeIcons.Download, contentDescription = null)
          }
        }
      }
    }
  }
}

@Composable
private fun ListHeader(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.titleSmall,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(start = 28.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
  )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TranslationCard(
  item: TranslationItem,
  downloading: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  actions: @Composable () -> Unit
) {
  val translation = item.translation
  val translator = translation.translatorNameLocalized?.takeIf { it.isNotBlank() }
    ?: translation.translator?.takeIf { it.isNotBlank() }
  val supporting = listOfNotNull(
    translator,
    translation.languageCode?.takeIf { it.isNotBlank() }?.uppercase()
  ).joinToString(" · ")

  SegmentedListItem(
    onClick = onClick,
    selected = false,
    shapes = ListItemDefaults.shapes(
      shape = RoundedCornerShape(24.dp),
      pressedShape = RoundedCornerShape(12.dp)
    ),
    colors = ListItemDefaults.segmentedColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ),
    modifier = modifier.padding(horizontal = 16.dp),
    leadingContent = {
      Icon(
        HomeIcons.Translate,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant
      )
    },
    supportingContent = {
      Column {
        if (item.needsUpgrade()) {
          Text(
            stringResource(R.string.update_available),
            color = MaterialTheme.colorScheme.tertiary,
            style = MaterialTheme.typography.labelMedium
          )
        } else if (supporting.isNotEmpty()) {
          Text(supporting, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (downloading) {
          LinearWavyProgressIndicator(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 8.dp)
          )
        }
      }
    },
    trailingContent = {
      Row(verticalAlignment = Alignment.CenterVertically) { actions() }
    }
  ) {
    Text(
      text = item.name(),
      style = MaterialTheme.typography.titleMedium,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis
    )
  }
}

/** Whether a translation matches what was typed: its name, translator or language. */
fun TranslationItem.matches(query: String): Boolean {
  if (query.isBlank()) return true
  val q = query.trim()
  return translation.displayName.contains(q, ignoreCase = true) ||
    translation.translator.orEmpty().contains(q, ignoreCase = true) ||
    translation.translatorNameLocalized.orEmpty().contains(q, ignoreCase = true) ||
    translation.languageCode.orEmpty().equals(q, ignoreCase = true)
}

/** A pill shaped search field, the same shape as the home screen's search bar. */
@Composable
fun TranslationFilterField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
  TextField(
    value = query,
    onValueChange = onQueryChange,
    singleLine = true,
    placeholder = { Text(stringResource(R.string.search_translations_hint)) },
    leadingIcon = { Icon(QuranIcons.Search, contentDescription = null) },
    trailingIcon = {
      if (query.isNotEmpty()) {
        IconButton(onClick = { onQueryChange("") }) {
          Icon(QuranIcons.Close, contentDescription = stringResource(com.quran.mobile.common.ui.core.R.string.cancel))
        }
      }
    },
    shape = CircleShape,
    colors = TextFieldDefaults.colors(
      focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
      unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
      focusedIndicatorColor = Color.Transparent,
      unfocusedIndicatorColor = Color.Transparent,
      disabledIndicatorColor = Color.Transparent
    ),
    modifier = modifier.fillMaxWidth()
  )
}
