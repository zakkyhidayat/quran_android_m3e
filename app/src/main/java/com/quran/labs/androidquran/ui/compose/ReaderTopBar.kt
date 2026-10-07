package com.quran.labs.androidquran.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.labs.androidquran.data.Constants

/** One translation in the picker: whether it is shown under the Arabic. */
class ReaderTranslationItem(val filename: String, val name: String, val checked: Boolean)

/** What the reader's top bar shows. The activity updates these as you turn pages. */
@Stable
class ReaderBarState {
  var title by mutableStateOf("")
  var subtitle by mutableStateOf("")
  var isBookmarked by mutableStateOf(false)
  var showingTranslation by mutableStateOf(false)
  var translations by mutableStateOf<List<ReaderTranslationItem>>(emptyList())
  var readingMode by mutableStateOf(Constants.READING_MODE_LIGHT)
  var arabicShown by mutableStateOf(true)

  /** A juz or hizb marker to say out loud for a moment, like "¼ Hizb 3". */
  var marker by mutableStateOf<String?>(null)

  /** Page, or the translation view with or without the Arabic above each translation. */
  val view: ReaderView
    get() = when {
      !showingTranslation -> ReaderView.PAGE
      arabicShown -> ReaderView.BOTH
      else -> ReaderView.TRANSLATION
    }
}

/** What the reader's top bar can trigger. The activity owns what each of these does. */
class ReaderBarActions(
  val onBack: () -> Unit,
  val onBookmark: () -> Unit,
  val onTranslationChecked: (filename: String) -> Unit,
  val onMoreTranslations: () -> Unit,
  val onReadingMode: (mode: String) -> Unit,
  val onSearch: () -> Unit,
  val onGoToPage: () -> Unit,
  val onSettings: () -> Unit,
  val onHelp: () -> Unit
)

/**
 * The reader's top bar: always visible above the page, with the surah, the page and where you are
 * in the Quran, and the actions you reach for while reading.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTopBar(state: ReaderBarState, actions: ReaderBarActions) {
  val ink = readingInk(state.readingMode)
  TopAppBar(
    title = { ReaderTitle(state, ink) },
    navigationIcon = {
      IconButton(onClick = actions.onBack) {
        Icon(QuranIcons.ArrowBack, contentDescription = stringResource(R.string.menu_back_to_page))
      }
    },
    actions = {
      IconButton(onClick = actions.onBookmark) {
        Icon(
          imageVector = if (state.isBookmarked) HomeIcons.BookmarkFilled else QuranIcons.BookmarkBorder,
          contentDescription = stringResource(R.string.menu_bookmarks),
          tint = if (state.isBookmarked) {
            if (state.readingMode == Constants.READING_MODE_NIGHT) Color(0xFF7FDBB0) else Color(0xFF176B4D)
          } else {
            ink
          }
        )
      }
      ReaderOverflowMenu(state, actions)
    },
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = Color.Transparent,
      scrolledContainerColor = Color.Transparent,
      titleContentColor = ink,
      navigationIconContentColor = ink,
      actionIconContentColor = ink
    )
  )
}

/** The reading mode's ink: dark on light and sepia paper, light on the night background. */
private fun readingInk(mode: String): Color = when (mode) {
  Constants.READING_MODE_NIGHT -> Color(0xFFE3DED3)
  Constants.READING_MODE_SEPIA -> Color(0xFF3B2F1E)
  else -> Color(0xFF1F1D17)
}

/** The surah, then the page and juz in smaller type. */
@Composable
private fun ReaderTitle(state: ReaderBarState, ink: Color) {
  Column {
    Text(
      text = state.title,
      style = MaterialTheme.typography.titleLarge,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    Text(
      text = state.subtitle,
      style = MaterialTheme.typography.bodySmall,
      color = ink.copy(alpha = 0.72f),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
private fun ReaderOverflowMenu(state: ReaderBarState, actions: ReaderBarActions) {
  var expanded by remember { mutableStateOf(false) }
  IconButton(onClick = { expanded = true }) {
    Icon(
      HomeIcons.MoreVert,
      contentDescription = stringResource(androidx.appcompat.R.string.abc_action_menu_overflow_description)
    )
  }
  val modes = listOf(
    Triple(Constants.READING_MODE_LIGHT, R.string.reading_mode_light, HomeIcons.LightMode),
    Triple(Constants.READING_MODE_SEPIA, R.string.reading_mode_sepia, HomeIcons.Contrast),
    Triple(Constants.READING_MODE_NIGHT, R.string.reading_mode_night, HomeIcons.DarkMode)
  ).map { (mode, label, icon) ->
    MenuEntry(stringResource(label), icon, selected = state.readingMode == mode) { actions.onReadingMode(mode) }
  }
  ExpressiveMenu(
    expanded = expanded,
    onDismiss = { expanded = false },
    sections = listOf(
      MenuSection(title = stringResource(R.string.prefs_reading_mode_title), entries = modes),
      MenuSection(
        entries = listOf(
          MenuEntry(stringResource(R.string.menu_search), QuranIcons.Search, onClick = actions.onSearch),
          MenuEntry(stringResource(R.string.menu_jump), HomeIcons.Numbers, onClick = actions.onGoToPage)
        )
      ),
      MenuSection(
        entries = listOf(
          MenuEntry(stringResource(R.string.menu_settings), HomeIcons.Settings, onClick = actions.onSettings),
          MenuEntry(stringResource(R.string.menu_help), HomeIcons.Help, onClick = actions.onHelp)
        )
      )
    )
  )
}
